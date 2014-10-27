package com.plusot.senselib.util;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.javacommon.util.StringUtil;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by peet on 02-10-14.
 */
public class Crypt {
    private static final String CLASSTAG = Crypt.class.getSimpleName();
    private static final String ss = "$67Ht_werSD";

    public static String hash(String seed) {
        byte[] bytes;
        try {

            MessageDigest md = MessageDigest.getInstance("MD5");
            MessageDigest sha = MessageDigest.getInstance("SHA");
            bytes = md.digest((ss + seed + ss).getBytes("UTF-8"));
            //printHex("md5", bytes);
            bytes = sha.digest((seed + StringUtil.toHexString(bytes)).getBytes("UTF-8"));
            String temp = StringUtil.toHexString(bytes);
            String result = temp.substring(5, 13).toLowerCase() + temp.substring(13, 16);

            return result;
        } catch (NoSuchAlgorithmException e) {
            LLog.e(Globals.TAG, CLASSTAG + ".hash", e);
        } catch (UnsupportedEncodingException e) {
            LLog.e(Globals.TAG, CLASSTAG + ".hash", e);
        }
        return "--hash--";
    }
}
