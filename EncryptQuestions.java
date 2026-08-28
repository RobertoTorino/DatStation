import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EncryptQuestions {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY = "12345678901234567890123456789012";
    private static final String IV = "1234567890123456";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java EncryptQuestions questions.json");
            return;
        }

        byte[] inputBytes = Files.readAllBytes(Paths.get(args[0]));
        SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes());

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] encryptedBytes = cipher.doFinal(inputBytes);
        Files.write(Paths.get("questions.dat"), encryptedBytes);

        System.out.println("Successfully encrypted to questions.dat");
    }
}
