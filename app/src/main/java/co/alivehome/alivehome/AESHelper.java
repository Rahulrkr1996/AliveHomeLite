package co.alivehome.alivehome;

/**
 * Created by Aman on 6/30/2017.
 */

import android.annotation.SuppressLint;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


@SuppressLint("NewApi")
public class AESHelper {
    private static final String tag = AESHelper.class.getSimpleName();

    private static final String characterEncoding = "UTF-8";
    private static final String cipherTransformation = "AES/CBC/PKCS5Padding";
    private static final String aesEncryptionAlgorithm = "AES";
    public static String key = "uerhfbdgeyrt";
    private static byte[] ivBytes = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
    private static byte[] keyBytes;

    private static AESHelper instance = null;


    AESHelper()
    {
        SecureRandom random = new SecureRandom();
        AESHelper.ivBytes = new byte[16];
        random.nextBytes(AESHelper.ivBytes);
    }

    public static AESHelper getInstance() {
        if(instance == null){
            instance = new AESHelper();
        }

        return instance;
    }

    public static String encrypt_string(final String plain) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException
    {
        return Base64.encodeToString(encrypt(plain.getBytes()), Base64.DEFAULT);
    }

    public static String decrypt_string(final String plain) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException, IOException
    {
        byte[] encryptedBytes = decrypt(Base64.decode(plain, 0));
        return new String(encryptedBytes);
    }



    public static byte[] encrypt(byte[] mes)
            throws NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException, IOException {

        keyBytes = key.getBytes("UTF-8");
        Log.d(tag,"Long KEY: "+keyBytes.length);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(keyBytes);
        keyBytes = md.digest();

        Log.d(tag,"Long KEY: "+keyBytes.length);

        AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        SecretKeySpec newKey = new SecretKeySpec(keyBytes, aesEncryptionAlgorithm);
        Cipher cipher = null;
        cipher = Cipher.getInstance(cipherTransformation);

        SecureRandom random = new SecureRandom();
        AESHelper.ivBytes = new byte[16];
        random.nextBytes(AESHelper.ivBytes);

        cipher.init(Cipher.ENCRYPT_MODE, newKey, random);
//    cipher.init(Cipher.ENCRYPT_MODE, newKey, ivSpec);
        byte[] destination = new byte[ivBytes.length + mes.length];
        System.arraycopy(ivBytes, 0, destination, 0, ivBytes.length);
        System.arraycopy(mes, 0, destination, ivBytes.length, mes.length);
        return  cipher.doFinal(destination);

    }

    public static byte[] decrypt(byte[] bytes)
            throws NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException, IOException, ClassNotFoundException {

        keyBytes = key.getBytes("UTF-8");
        Log.d(tag,"Long KEY: "+keyBytes.length);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(keyBytes);
        keyBytes = md.digest();
        Log.d(tag,"Long KEY: "+keyBytes.length);

        byte[] ivB = Arrays.copyOfRange(bytes,0,16);
        Log.d(tag, "IV: "+new String(ivB));
        byte[] codB = Arrays.copyOfRange(bytes,16,bytes.length);


        AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivB);
        SecretKeySpec newKey = new SecretKeySpec(keyBytes, aesEncryptionAlgorithm);
        Cipher cipher = Cipher.getInstance(cipherTransformation);
        cipher.init(Cipher.DECRYPT_MODE, newKey, ivSpec);
        byte[] res = cipher.doFinal(codB);
        return  res;

    }
}
