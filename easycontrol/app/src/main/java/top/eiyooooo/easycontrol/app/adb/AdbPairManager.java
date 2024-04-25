package top.eiyooooo.easycontrol.app.adb;

import android.os.Build;
import android.sun.misc.BASE64Encoder;
import android.sun.security.provider.X509Factory;
import android.sun.security.x509.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.muntashirakon.adb.AbsAdbConnectionManager;
import top.eiyooooo.easycontrol.app.entity.AppData;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.Random;

public class AdbPairManager extends AbsAdbConnectionManager {
    public static AbsAdbConnectionManager INSTANCE;

    public static void init() throws Exception {
        INSTANCE = new AdbPairManager();
    }

    public static String keyName;
    private PrivateKey mPrivateKey;
    private Certificate mCertificate;

    private AdbPairManager() throws Exception {
        setApi(Build.VERSION_CODES.R);
        keyName = "Easycontrol_For_Car-unknown";
        mPrivateKey = readPrivateKeyFromFile();
        mCertificate = readCertificateFromFile();
        if (mPrivateKey != null && mCertificate != null) {
            keyName = getKeyName(mPrivateKey);
        } else {
            int keySize = 2048;
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(keySize, SecureRandom.getInstance("SHA1PRNG"));
            KeyPair generateKeyPair = keyPairGenerator.generateKeyPair();
            PublicKey publicKey = generateKeyPair.getPublic();
            mPrivateKey = generateKeyPair.getPrivate();

            keyName = getKeyName(mPrivateKey);
            String subject = "CN=" + keyName;
            String algorithmName = "SHA512withRSA";
            long expiryDate = System.currentTimeMillis() + 86400000;
            CertificateExtensions certificateExtensions = new CertificateExtensions();
            certificateExtensions.set("SubjectKeyIdentifier", new SubjectKeyIdentifierExtension(
                    new KeyIdentifier(publicKey).getIdentifier()));
            X500Name x500Name = new X500Name(subject);
            Date notBefore = new Date();
            Date notAfter = new Date(expiryDate);
            certificateExtensions.set("PrivateKeyUsage", new PrivateKeyUsageExtension(notBefore, notAfter));
            CertificateValidity certificateValidity = new CertificateValidity(notBefore, notAfter);
            X509CertInfo x509CertInfo = new X509CertInfo();
            x509CertInfo.set("version", new CertificateVersion(2));
            x509CertInfo.set("serialNumber", new CertificateSerialNumber(new Random().nextInt() & Integer.MAX_VALUE));
            x509CertInfo.set("algorithmID", new CertificateAlgorithmId(AlgorithmId.get(algorithmName)));
            x509CertInfo.set("subject", new CertificateSubjectName(x500Name));
            x509CertInfo.set("key", new CertificateX509Key(publicKey));
            x509CertInfo.set("validity", certificateValidity);
            x509CertInfo.set("issuer", new CertificateIssuerName(x500Name));
            x509CertInfo.set("extensions", certificateExtensions);
            X509CertImpl x509CertImpl = new X509CertImpl(x509CertInfo);
            x509CertImpl.sign(mPrivateKey, algorithmName);
            mCertificate = x509CertImpl;

            writePrivateKeyToFile(mPrivateKey);
            writeCertificateToFile(mCertificate);
        }
    }

    public static void regenerateKey() throws Exception {
        INSTANCE.close();
        File privateKeyFile = new File(AppData.main.getFilesDir(), "pair_private.key");
        File certFile = new File(AppData.main.getFilesDir(), "pair_cert.pem");
        if (privateKeyFile.exists()) {
            boolean ignored = privateKeyFile.delete();
        }
        if (certFile.exists()) {
            boolean ignored = certFile.delete();
        }
        INSTANCE = new AdbPairManager();
    }

    @NonNull
    @Override
    protected PrivateKey getPrivateKey() {
        return mPrivateKey;
    }

    @NonNull
    @Override
    protected Certificate getCertificate() {
        return mCertificate;
    }

    @NonNull
    @Override
    protected String getDeviceName() {
        return keyName;
    }

    @Nullable
    private static Certificate readCertificateFromFile()
            throws IOException, CertificateException {
        File certFile = new File(AppData.main.getFilesDir(), "pair_cert.pem");
        if (!certFile.exists()) return null;
        try (FileInputStream cert = new FileInputStream(certFile)) {
            return CertificateFactory.getInstance("X.509").generateCertificate(cert);
        }
    }

    private static void writeCertificateToFile(@NonNull Certificate certificate)
            throws CertificateEncodingException, IOException {
        File certFile = new File(AppData.main.getFilesDir(), "pair_cert.pem");
        BASE64Encoder encoder = new BASE64Encoder();
        try (FileOutputStream os = new FileOutputStream(certFile)) {
            os.write(X509Factory.BEGIN_CERT.getBytes(StandardCharsets.UTF_8));
            os.write('\n');
            encoder.encode(certificate.getEncoded(), os);
            os.write('\n');
            os.write(X509Factory.END_CERT.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Nullable
    private static PrivateKey readPrivateKeyFromFile()
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        File privateKeyFile = new File(AppData.main.getFilesDir(), "pair_private.key");
        if (!privateKeyFile.exists()) return null;
        byte[] privateKeyBytes = new byte[(int) privateKeyFile.length()];
        try (FileInputStream is = new FileInputStream(privateKeyFile)) {
            int ignored = is.read(privateKeyBytes);
        }
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        return keyFactory.generatePrivate(privateKeySpec);
    }

    private static void writePrivateKeyToFile(@NonNull PrivateKey privateKey)
            throws IOException {
        File privateKeyFile = new File(AppData.main.getFilesDir(), "pair_private.key");
        try (FileOutputStream os = new FileOutputStream(privateKeyFile)) {
            os.write(privateKey.getEncoded());
        }
    }

    private static String getKeyName(@NonNull PrivateKey privateKey) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
        int hashCode = keyFactory.generatePrivate(privateKeySpec).hashCode();
        String hash = hashCode >= 0 ? String.valueOf(hashCode) : String.valueOf(-hashCode);
        if (hash.length() <= 4) return "Easycontrol_For_Car-" + hash;
        else return "Easycontrol_For_Car-" + hash.substring(hash.length() - 4);
    }
}