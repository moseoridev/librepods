import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

// Source-file launcher fallback when the SDK is on an unmounted drive.
class SignApk {
    public static void main(String[] args) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        try (var input = new FileInputStream(args[0])) {
            store.load(input, "android".toCharArray());
        }
        var signer = new ApkSigner.SignerConfig.Builder("androiddebugkey",
                (PrivateKey) store.getKey("androiddebugkey", "android".toCharArray()),
                List.of((X509Certificate) store.getCertificate("androiddebugkey"))).build();
        new ApkSigner.Builder(List.of(signer)).setInputApk(new File(args[1]))
                .setOutputApk(new File(args[2])).setV4SigningEnabled(false).build().sign();
        var result = new ApkVerifier.Builder(new File(args[2])).build().verify();
        if (!result.isVerified() || !result.isVerifiedUsingV2Scheme()) {
            throw new IllegalStateException("APK signature verification failed: " + result.getErrors());
        }
    }
}
