package top.eiyooooo.easycontrol.app.entity;

public class Device {
  public static final int TYPE_NORMAL = 1;
  public static final int TYPE_LINK = 2;

  public final String uuid;
  public final int type;
  public String name;
  public String address;
  public String specified_app;
  public boolean isAudio;
  public int maxSize;
  public int maxFps;
  public int maxVideoBit;
  public boolean setResolution;
  public boolean defaultFull;
  public boolean useH265;
  public boolean useOpus;
  public boolean connectOnStart;
  public boolean clipboardSync;
  public boolean nightModeSync;
  public int connection = -1; // -1:未检查连接 0:检查连接中 1:可以连接 2:无法连接

  public Device(String uuid,
                int type,
                String name,
                String address,
                String specified_app,
                boolean isAudio,
                int maxSize,
                int maxFps,
                int maxVideoBit,
                boolean setResolution,
                boolean defaultFull,
                boolean useH265,
                boolean useOpus,
                boolean connectOnStart,
                boolean clipboardSync,
                boolean nightModeSync) {
    this.uuid = uuid;
    this.type = type;
    this.name = name;
    this.address = address;
    this.specified_app = specified_app;
    this.isAudio = isAudio;
    this.maxSize = maxSize;
    this.maxFps = maxFps;
    this.maxVideoBit = maxVideoBit;
    this.setResolution = setResolution;
    this.defaultFull = defaultFull;
    this.useH265 = useH265;
    this.useOpus = useOpus;
    this.connectOnStart = connectOnStart;
    this.clipboardSync = clipboardSync;
    this.nightModeSync = nightModeSync;
  }

  public Device(String uuid, int type) {
    this.uuid = uuid;
    this.type = type;
  }

  public static void copyDevice(Device source, Device target) {
    target.name = source.name;
    target.address = source.address;
    target.specified_app = source.specified_app;
    target.isAudio = source.isAudio;
    target.maxSize = source.maxSize;
    target.maxFps = source.maxFps;
    target.maxVideoBit = source.maxVideoBit;
    target.setResolution = source.setResolution;
    target.defaultFull = source.defaultFull;
    target.useH265 = source.useH265;
    target.useOpus = source.useOpus;
    target.connectOnStart = source.connectOnStart;
    target.clipboardSync = source.clipboardSync;
    target.nightModeSync = source.nightModeSync;
  }

  public static Device getDefaultDevice(String uuid, int type) {
    return new Device(uuid, type, uuid, "", "", AppData.setting.getDefaultIsAudio(), AppData.setting.getDefaultMaxSize(), AppData.setting.getDefaultMaxFps(), AppData.setting.getDefaultMaxVideoBit(), AppData.setting.getDefaultSetResolution(), true, AppData.setting.getDefaultUseH265(), AppData.setting.getDefaultUseOpus(), false, AppData.setting.getDefaultClipboardSync(), AppData.setting.getDefaultNightModeSync());
  }

  public boolean isNormalDevice() {
    return type == TYPE_NORMAL;
  }

  public boolean isLinkDevice() {
    return type == TYPE_LINK;
  }

}
