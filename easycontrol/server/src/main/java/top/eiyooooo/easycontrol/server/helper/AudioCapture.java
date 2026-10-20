package top.eiyooooo.easycontrol.server.helper;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.AttributionSource;
import android.content.Context;
import android.media.*;
import android.os.Build;
import android.os.Looper;
import android.os.Parcel;
import top.eiyooooo.easycontrol.server.entity.Options;
import top.eiyooooo.easycontrol.server.utils.L;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class AudioCapture {

    public static final int SAMPLE_RATE = 48000;
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    public static final int CHANNELS = 2;
    public static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_LEFT | AudioFormat.CHANNEL_IN_RIGHT;
    public static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public static final int BYTES_PER_SAMPLE = 2;

    public static AudioRecord init() {
        AudioRecord recorder;
        // Android 16 开始限制通过 REMOTE_SUBMIX 采集播放音频（scrcpy #5698/#6021），优先使用 AudioPolicy 混音采集
        if (Build.VERSION.SDK_INT >= 36) {
            try {
                recorder = createAudioRecordByAudioPolicy();
                recorder.startRecording();
                return recorder;
            } catch (Throwable e) {
                L.w("Cannot create AudioRecord by AudioPolicy, fallback to REMOTE_SUBMIX", e);
            }
        }
        try {
            recorder = createAudioRecord();
        } catch (NullPointerException e) {
            L.w("Cannot create AudioRecord, try workaround");
            recorder = createAudioRecordWorkaround();
        }
        recorder.startRecording();
        return recorder;
    }

    public static int millisToBytes(int millis) {
        return (SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE / 1000) * millis;
    }

    // 必须持有 policy 引用，否则会被系统注销导致采集失效
    private static Object registeredAudioPolicy;

    // uid 设备亲和成功时记录，release 时必须清除，否则该应用的播放会一直路由到已销毁的采集设备
    private static AudioManager affinityAudioManager;
    private static int affinityUid = -1;

    private static Class<?> mixingRuleClass, audioMixClass, audioPolicyClass;

    // Android 16+ 通过 AudioPolicy 混音采集播放音频（参考 scrcpy audio/AudioPlaybackCapture）。
    // 流转指定应用时（Options.audioPackage 非空）按 uid 过滤：native AudioMix 匹配规则只认
    // usage/capture-preset、不支持 uid，因此用"永不命中的占位规则 + setUidDeviceAffinities
    // 把该应用的播放强制路由进采集流"实现按应用分流，其他应用声音留在被控端；失败回退全局混音。
    @SuppressLint({"WrongConstant", "MissingPermission"})
    private static AudioRecord createAudioRecordByAudioPolicy() throws Exception {
        Integer captureUid = Options.audioPackage.isEmpty() ? null : getPackageUid(Options.audioPackage);
        if (captureUid != null) {
            AudioRecord uidRecord = null;
            try {
                uidRecord = createAudioRecordByUidAffinity(captureUid);
                return uidRecord;
            } catch (Throwable e) {
                L.w("uid affinity capture failed, fallback to global mix", e);
                if (uidRecord != null) {
                    try {
                        uidRecord.release();
                    } catch (Exception ignored) {
                    }
                }
                unregisterPolicy();
            }
        }
        return createAudioRecordByGlobalRule();
    }

    private static void resolvePolicyClasses() throws ClassNotFoundException {
        mixingRuleClass = Class.forName("android.media.audiopolicy.AudioMixingRule");
        audioMixClass = Class.forName("android.media.audiopolicy.AudioMix");
        audioPolicyClass = Class.forName("android.media.audiopolicy.AudioPolicy");
    }

    private static AudioFormat buildCaptureFormat() {
        return new AudioFormat.Builder()
                .setEncoding(ENCODING)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG)
                .build();
    }

    // 回退：注销已注册的 policy（uid 亲和路径失败时清理，避免残留 mix 劫持路由）
    private static void unregisterPolicy() {
        if (registeredAudioPolicy == null) return;
        try {
            AudioManager.class.getMethod("unregisterAudioPolicyStatic", audioPolicyClass)
                    .invoke(null, registeredAudioPolicy);
        } catch (Exception e) {
            L.e("unregisterAudioPolicyStatic", e);
        }
        registeredAudioPolicy = null;
    }

    // 全局混音：采集所有媒体类播放，其他应用声音也会传到控制端
    @SuppressLint({"WrongConstant", "MissingPermission"})
    private static AudioRecord createAudioRecordByGlobalRule() throws Exception {
        resolvePolicyClasses();
        Class<?> mixingRuleBuilderClass = Class.forName("android.media.audiopolicy.AudioMixingRule$Builder");
        Class<?> audioMixBuilderClass = Class.forName("android.media.audiopolicy.AudioMix$Builder");
        Class<?> audioPolicyBuilderClass = Class.forName("android.media.audiopolicy.AudioPolicy$Builder");

        // 构建 AudioMixingRule
        Object ruleBuilder = mixingRuleBuilderClass.getDeclaredConstructor().newInstance();
        mixingRuleBuilderClass.getMethod("setTargetMixRole", int.class)
                .invoke(ruleBuilder, getStaticInt(mixingRuleClass, "MIX_ROLE_PLAYERS", 0));
        Method addMixRuleMethod = mixingRuleBuilderClass.getMethod("addMixRule", int.class, Object.class);
        int usageRule = getStaticInt(mixingRuleClass, "RULE_MATCH_ATTRIBUTE_USAGE", 1);
        int[] usages = {AudioAttributes.USAGE_UNKNOWN, AudioAttributes.USAGE_MEDIA, AudioAttributes.USAGE_GAME};
        for (int usage : usages) {
            AudioAttributes attributes = new AudioAttributes.Builder().setUsage(usage).build();
            addMixRuleMethod.invoke(ruleBuilder, usageRule, attributes);
        }
        // 允许采集语音通信音频（部分 Android 16 版本新增，低版本无此方法）
        try {
            mixingRuleBuilderClass.getMethod("voiceCommunicationCaptureAllowed", boolean.class).invoke(ruleBuilder, true);
        } catch (NoSuchMethodException ignored) {
        }
        Object mixingRule = mixingRuleBuilderClass.getMethod("build").invoke(ruleBuilder);

        Object audioMix = buildAudioMix(mixingRuleBuilderClass, audioMixBuilderClass, mixingRule, routeFlags());
        return buildAndRegisterAudioRecord(audioPolicyBuilderClass, audioMix);
    }

    private static int routeFlags() {
        return getStaticInt(audioMixClass, "ROUTE_FLAG_LOOP_BACK", 2) | getStaticInt(audioMixClass, "ROUTE_FLAG_RENDER", 1);
    }

    @SuppressWarnings("unchecked")
    private static Object buildAudioMix(Class<?> mixingRuleBuilderClass, Class<?> audioMixBuilderClass,
                                        Object mixingRule, int routeFlags) throws Exception {
        // AudioMix，ROUTE_FLAG_LOOP_BACK 采集且 ROUTE_FLAG_RENDER 保证设备继续外放
        Constructor<?> mixBuilderConstructor = audioMixBuilderClass.getDeclaredConstructor(mixingRuleClass);
        mixBuilderConstructor.setAccessible(true);
        Object mixBuilder = mixBuilderConstructor.newInstance(mixingRule);
        audioMixBuilderClass.getMethod("setRouteFlags", int.class).invoke(mixBuilder, routeFlags);
        // 尽量对齐编码器格式（48kHz/双声道/PCM16）
        try {
            audioMixBuilderClass.getMethod("setFormat", AudioFormat.class).invoke(mixBuilder, buildCaptureFormat());
        } catch (NoSuchMethodException ignored) {
        }
        return audioMixBuilderClass.getMethod("build").invoke(mixBuilder);
    }

    // 按 uid 过滤：mix 用不会命中的占位规则（播放侧无 capture preset，永不匹配任何播放），
    // 流转应用的播放经 setUidDeviceAffinities 强制路由到本 mix 的 REMOTE_SUBMIX 设备进入采集流，
    // 其他应用正常走扬声器、不进混音，控制端只听得到流转应用的声音
    @SuppressLint({"WrongConstant", "MissingPermission"})
    private static AudioRecord createAudioRecordByUidAffinity(int uid) throws Exception {
        resolvePolicyClasses();
        Class<?> mixingRuleBuilderClass = Class.forName("android.media.audiopolicy.AudioMixingRule$Builder");
        Class<?> audioMixBuilderClass = Class.forName("android.media.audiopolicy.AudioMix$Builder");
        Class<?> audioPolicyBuilderClass = Class.forName("android.media.audiopolicy.AudioPolicy$Builder");

        AudioManager audioManager = (AudioManager) FakeContext.get().getSystemService(Context.AUDIO_SERVICE);
        // 记录注册前已存在的 REMOTE_SUBMIX 输出设备地址，注册后新出现的就是本 mix 的设备
        HashSet<String> before = new HashSet<>();
        for (AudioDeviceInfo info : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if (info.getType() == AudioDeviceInfo.TYPE_REMOTE_SUBMIX) before.add(info.getAddress());
        }

        // 占位规则：AudioMixingRule.build 要求至少一条规则，用播放侧不存在的 capture preset
        Object ruleBuilder = mixingRuleBuilderClass.getDeclaredConstructor().newInstance();
        mixingRuleBuilderClass.getMethod("setTargetMixRole", int.class)
                .invoke(ruleBuilder, getStaticInt(mixingRuleClass, "MIX_ROLE_PLAYERS", 0));
        Method addMixRuleMethod = mixingRuleBuilderClass.getMethod("addMixRule", int.class, Object.class);
        int presetRule = getStaticInt(mixingRuleClass, "RULE_MATCH_ATTRIBUTE_CAPTURE_PRESET", 2);
        AudioAttributes.Builder placeholderBuilder = new AudioAttributes.Builder();
        // setCapturePreset 为 @hide，播放侧属性不存在 capture preset，该规则永不匹配
        placeholderBuilder.getClass().getMethod("setCapturePreset", int.class)
                .invoke(placeholderBuilder, MediaRecorder.AudioSource.UNPROCESSED);
        AudioAttributes placeholder = placeholderBuilder.build();
        addMixRuleMethod.invoke(ruleBuilder, presetRule, placeholder);
        Object mixingRule = mixingRuleBuilderClass.getMethod("build").invoke(ruleBuilder);

        Object audioMix = buildAudioMix(mixingRuleBuilderClass, audioMixBuilderClass, mixingRule, routeFlags());
        AudioRecord record = buildAndRegisterAudioRecord(audioPolicyBuilderClass, audioMix);

        // 找到本 mix 注册后新建的 REMOTE_SUBMIX 输出设备
        AudioDeviceInfo target = null;
        for (AudioDeviceInfo info : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if (info.getType() == AudioDeviceInfo.TYPE_REMOTE_SUBMIX && !before.contains(info.getAddress())) {
                target = info;
                break;
            }
        }
        if (target == null) throw new Exception("remote submix output device of mix not found");

        // setUidDeviceAffinities(uid, List<AudioDevice>)：把该应用的播放强制路由到采集设备
        Class<?> audioDeviceClass = Class.forName("android.media.AudioDevice");
        Object audioDevice = audioDeviceClass.getDeclaredConstructor(int.class, AudioDeviceInfo.class)
                .newInstance(getStaticInt(audioDeviceClass, "ROLE_OUTPUT", 1), target);
        ArrayList<Object> devices = new ArrayList<>();
        devices.add(audioDevice);
        audioManager.getClass().getMethod("setUidDeviceAffinities", int.class, List.class)
                .invoke(audioManager, uid, devices);
        affinityAudioManager = audioManager;
        affinityUid = uid;
        L.d("audio capture by uid affinity: uid=" + uid + " device=" + target.getAddress());
        return record;
    }

    @SuppressLint({"WrongConstant", "MissingPermission"})
    private static AudioRecord buildAndRegisterAudioRecord(Class<?> audioPolicyBuilderClass, Object audioMix) throws Exception {
        // 构建 AudioPolicy
        Object policyBuilder = audioPolicyBuilderClass.getDeclaredConstructor(Context.class).newInstance(FakeContext.get());
        audioPolicyBuilderClass.getMethod("addMix", audioMixClass).invoke(policyBuilder, audioMix);
        Object policy = audioPolicyBuilderClass.getMethod("build").invoke(policyBuilder);

        // 注册 policy 并创建 AudioRecord
        AudioManager.class.getMethod("registerAudioPolicyStatic", audioPolicyClass).invoke(null, policy);
        AudioRecord audioRecord = (AudioRecord) audioPolicyClass.getMethod("createAudioRecordSink", audioMixClass)
                .invoke(policy, audioMix);
        registeredAudioPolicy = policy;
        return audioRecord;
    }

    // 会话结束释放 uid 亲和，恢复该应用的正常路由
    public static void releaseAffinity() {
        if (affinityAudioManager == null) return;
        try {
            affinityAudioManager.getClass().getMethod("removeUidDeviceAffinities", int.class)
                    .invoke(affinityAudioManager, affinityUid);
            L.d("uid affinity removed: uid=" + affinityUid);
        } catch (Exception e) {
            L.e("removeUidDeviceAffinities", e);
        }
        affinityAudioManager = null;
        affinityUid = -1;
    }

    private static int getStaticInt(Class<?> cls, String fieldName, int defaultValue) {
        try {
            return cls.getField(fieldName).getInt(null);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // 查询应用的 uid（shell 权限可查任意包），失败返回 null 由调用方回退全局混音
    private static Integer getPackageUid(String packageName) {
        try {
            return FakeContext.get().getPackageManager().getApplicationInfo(packageName, 0).uid;
        } catch (Exception e) {
            L.w("getPackageUid " + packageName, e);
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @SuppressLint({"WrongConstant", "MissingPermission"})
    private static AudioRecord createAudioRecord() {
        AudioRecord.Builder audioRecordBuilder = new AudioRecord.Builder();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) audioRecordBuilder.setContext(FakeContext.get());

        audioRecordBuilder.setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX);
        AudioFormat.Builder audioFormatBuilder = new AudioFormat.Builder();
        audioFormatBuilder.setEncoding(ENCODING);
        audioFormatBuilder.setSampleRate(SAMPLE_RATE);
        audioFormatBuilder.setChannelMask(CHANNEL_CONFIG);
        audioRecordBuilder.setAudioFormat(audioFormatBuilder.build());
        audioRecordBuilder.setBufferSizeInBytes(16 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING));
        return audioRecordBuilder.build();
    }

    @TargetApi(Build.VERSION_CODES.R)
    @SuppressLint("WrongConstant,MissingPermission,BlockedPrivateApi,SoonBlockedPrivateApi,DiscouragedPrivateApi")
    private static AudioRecord createAudioRecordWorkaround() {
        try {
            // AudioRecord audioRecord = new AudioRecord(0L);
            Constructor<AudioRecord> audioRecordConstructor = AudioRecord.class.getDeclaredConstructor(long.class);
            audioRecordConstructor.setAccessible(true);
            AudioRecord audioRecord = audioRecordConstructor.newInstance(0L);

            // audioRecord.mRecordingState = RECORDSTATE_STOPPED;
            Field mRecordingStateField = AudioRecord.class.getDeclaredField("mRecordingState");
            mRecordingStateField.setAccessible(true);
            mRecordingStateField.set(audioRecord, AudioRecord.RECORDSTATE_STOPPED);

            Looper looper = Looper.myLooper();
            if (looper == null) {
                looper = Looper.getMainLooper();
            }

            // audioRecord.mInitializationLooper = looper;
            Field mInitializationLooperField = AudioRecord.class.getDeclaredField("mInitializationLooper");
            mInitializationLooperField.setAccessible(true);
            mInitializationLooperField.set(audioRecord, looper);

            // Create `AudioAttributes` with fixed capture preset
            AudioAttributes.Builder audioAttributesBuilder = new AudioAttributes.Builder();
            Method setInternalCapturePresetMethod = AudioAttributes.Builder.class.getMethod("setInternalCapturePreset", int.class);
            setInternalCapturePresetMethod.invoke(audioAttributesBuilder, MediaRecorder.AudioSource.REMOTE_SUBMIX);
            AudioAttributes attributes = audioAttributesBuilder.build();

            // audioRecord.mAudioAttributes = attributes;
            Field mAudioAttributesField = AudioRecord.class.getDeclaredField("mAudioAttributes");
            mAudioAttributesField.setAccessible(true);
            mAudioAttributesField.set(audioRecord, attributes);

            // audioRecord.audioParamCheck(capturePreset, sampleRate, encoding);
            Method audioParamCheckMethod = AudioRecord.class.getDeclaredMethod("audioParamCheck", int.class, int.class, int.class);
            audioParamCheckMethod.setAccessible(true);
            audioParamCheckMethod.invoke(audioRecord, MediaRecorder.AudioSource.REMOTE_SUBMIX, SAMPLE_RATE, ENCODING);

            // audioRecord.mChannelCount = channels
            Field mChannelCountField = AudioRecord.class.getDeclaredField("mChannelCount");
            mChannelCountField.setAccessible(true);
            mChannelCountField.set(audioRecord, CHANNELS);

            // audioRecord.mChannelMask = channelMask
            Field mChannelMaskField = AudioRecord.class.getDeclaredField("mChannelMask");
            mChannelMaskField.setAccessible(true);
            mChannelMaskField.set(audioRecord, CHANNEL_MASK);

            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING);
            int bufferSizeInBytes = minBufferSize * 8;

            // audioRecord.audioBuffSizeCheck(bufferSizeInBytes)
            Method audioBuffSizeCheckMethod = AudioRecord.class.getDeclaredMethod("audioBuffSizeCheck", int.class);
            audioBuffSizeCheckMethod.setAccessible(true);
            audioBuffSizeCheckMethod.invoke(audioRecord, bufferSizeInBytes);

            final int channelIndexMask = 0;

            int[] sampleRateArray = new int[]{SAMPLE_RATE};
            int[] session = new int[]{AudioManager.AUDIO_SESSION_ID_GENERATE};

            int initResult;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                // private native final int native_setup(Object audiorecord_this,
                // Object /*AudioAttributes*/ attributes,
                // int[] sampleRate, int channelMask, int channelIndexMask, int audioFormat,
                // int buffSizeInBytes, int[] sessionId, String opPackageName,
                // long nativeRecordInJavaObj);
                Method nativeSetupMethod = AudioRecord.class.getDeclaredMethod("native_setup", Object.class, Object.class, int[].class, int.class,
                        int.class, int.class, int.class, int[].class, String.class, long.class);
                nativeSetupMethod.setAccessible(true);
                initResult = (int) nativeSetupMethod.invoke(audioRecord, new WeakReference<AudioRecord>(audioRecord), attributes, sampleRateArray,
                        CHANNEL_MASK, channelIndexMask, audioRecord.getAudioFormat(), bufferSizeInBytes, session, FakeContext.get().getOpPackageName(),
                        0L);
            } else {
                // Assume `context` is never `null`
                AttributionSource attributionSource = FakeContext.get().getAttributionSource();

                // Assume `attributionSource.getPackageName()` is never null

                // ScopedParcelState attributionSourceState = attributionSource.asScopedParcelState()
                Method asScopedParcelStateMethod = AttributionSource.class.getDeclaredMethod("asScopedParcelState");
                asScopedParcelStateMethod.setAccessible(true);

                try (AutoCloseable attributionSourceState = (AutoCloseable) asScopedParcelStateMethod.invoke(attributionSource)) {
                    Method getParcelMethod = attributionSourceState.getClass().getDeclaredMethod("getParcel");
                    Parcel attributionSourceParcel = (Parcel) getParcelMethod.invoke(attributionSourceState);

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        // private native int native_setup(Object audiorecordThis,
                        // Object /*AudioAttributes*/ attributes,
                        // int[] sampleRate, int channelMask, int channelIndexMask, int audioFormat,
                        // int buffSizeInBytes, int[] sessionId, @NonNull Parcel attributionSource,
                        // long nativeRecordInJavaObj, int maxSharedAudioHistoryMs);
                        Method nativeSetupMethod = AudioRecord.class.getDeclaredMethod("native_setup", Object.class, Object.class, int[].class,
                                int.class, int.class, int.class, int.class, int[].class, Parcel.class, long.class, int.class);
                        nativeSetupMethod.setAccessible(true);
                        initResult = (int) nativeSetupMethod.invoke(audioRecord, new WeakReference<AudioRecord>(audioRecord), attributes,
                                sampleRateArray, CHANNEL_MASK, channelIndexMask, audioRecord.getAudioFormat(), bufferSizeInBytes, session,
                                attributionSourceParcel, 0L, 0);
                    } else {
                        // Android 14 added a new int parameter "halInputFlags"
                        // <https://github.com/aosp-mirror/platform_frameworks_base/commit/f6135d75db79b1d48fad3a3b3080d37be20a2313>
                        Method nativeSetupMethod = AudioRecord.class.getDeclaredMethod("native_setup", Object.class, Object.class, int[].class,
                                int.class, int.class, int.class, int.class, int[].class, Parcel.class, long.class, int.class, int.class);
                        nativeSetupMethod.setAccessible(true);
                        initResult = (int) nativeSetupMethod.invoke(audioRecord, new WeakReference<AudioRecord>(audioRecord), attributes,
                                sampleRateArray, CHANNEL_MASK, channelIndexMask, audioRecord.getAudioFormat(), bufferSizeInBytes, session,
                                attributionSourceParcel, 0L, 0, 0);
                    }
                }
            }

            if (initResult != AudioRecord.SUCCESS) {
                L.w("AudioRecord initialization failed with code " + initResult);
                throw new RuntimeException("Cannot create AudioRecord");
            }

            // mSampleRate = sampleRate[0]
            Field mSampleRateField = AudioRecord.class.getDeclaredField("mSampleRate");
            mSampleRateField.setAccessible(true);
            mSampleRateField.set(audioRecord, sampleRateArray[0]);

            // audioRecord.mSessionId = session[0]
            Field mSessionIdField = AudioRecord.class.getDeclaredField("mSessionId");
            mSessionIdField.setAccessible(true);
            mSessionIdField.set(audioRecord, session[0]);

            // audioRecord.mState = AudioRecord.STATE_INITIALIZED
            Field mStateField = AudioRecord.class.getDeclaredField("mState");
            mStateField.setAccessible(true);
            mStateField.set(audioRecord, AudioRecord.STATE_INITIALIZED);

            return audioRecord;
        } catch (Exception e) {
            L.e("Cannot create AudioRecord", e);
            throw new RuntimeException("Cannot create AudioRecord");
        }
    }
}
