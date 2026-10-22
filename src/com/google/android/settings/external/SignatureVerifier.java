package com.google.android.settings.external;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

/** Checks that a caller is an allowlisted Google package signed with the expected key. */
public abstract class SignatureVerifier {
    private static final String TAG = "SignatureVerifier";

    private static final byte[] DEBUG_DIGEST_GMSCORE =
            digest("1975b2f17177bc89a5dff31f9e64a6cae281a53dc1d1d59b1d147fe1c82afa00");
    private static final byte[] RELEASE_DIGEST_GMSCORE =
            digest("f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83");
    private static final byte[] DEBUG_DIGEST_TIPS =
            digest("ab18e82c61a2d58bd7181477bc9f75a8214d1762739025ac2491091411b84fb3");
    private static final byte[] RELEASE_DIGEST_TIPS =
            digest("0e4479fe193d01cd46215fccd0d9233dec77fea259fbcc9f092119f50a8372e5");
    private static final byte[] DEBUG_DIGEST_LAUNCHER =
            digest("4b4d9abde8f3d6987558c76e261e6fe9d3c7cc299ebef22d56df6321dbae3562");
    private static final byte[] RELEASE_DIGEST_LAUNCHER =
            digest("a86bdb059f28f265162d64ce6c8d9772901d227e741581d1f04a5ed132a574d0");
    private static final byte[] DEBUG_DIGEST_ASSISTANT_DEV_APP =
            digest("1975b2f17177bc89a5dff31f9e64a6cae281a53dc1d1d59b1d147fe1c82afa00");
    private static final byte[] ROUTER_TEST_APP =
            digest("103938ee4537e59e8ee792f654504fb8346fc6b346d0bbc4415fc339fcfc8ec1");
    private static final byte[] DEBUG_DIGEST_PIXEL_TROUBLESHOOTING =
            digest("5d21b4b1513d0c61b1886ffbbe0d2f2df751b930da38726c9b060f0e5b0ec30b");
    private static final byte[] RELEASE_DIGEST_PIXEL_TROUBLESHOOTING =
            digest("0283d22656e555a53ae543eb304cdb6669ea24d495dd93f1e8682be8095ec95a");

    private static final String PACKAGE_GSA = "com.google.android.googlequicksearchbox";
    private static final String PACKAGE_GMSCORE = "com.google.android.gms";
    private static final String PACKAGE_TIPS = "com.google.android.apps.tips";
    private static final String PACKAGE_LAUNCHER = "com.google.android.apps.nexuslauncher";
    private static final String PACKAGE_PIXEL_TROUBLESHOOTING =
            "com.google.android.apps.pixel.support";
    private static final String PACKAGE_ASSISTANT_DEV_APP =
            "com.google.android.apps.search.assistant.surfaces.voice.devapp";
    private static final String PACKAGE_SETTINGS_API_TESTER =
            "com.google.android.settings.api.tester";
    private static final String PACKAGE_ROUTER_TEST_APP = "com.android.settingslib.router.testapp";

    private static byte[] digest(String hex) {
        return HexFormat.of().parseHex(hex);
    }

    public static boolean isPackageAllowlisted(Context context, String packageName) {
        try {
            PackageInfo packageInfo =
                    context.getPackageManager()
                            .getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            String name = packageInfo.packageName;
            if (!verifyAllowlistedPackage(name)) {
                Log.e(TAG, "Package name: " + name + " is not allowlisted.");
                return false;
            }
            return isSignatureAllowlisted(packageInfo);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not find package name.", e);
            return false;
        }
    }

    private static boolean isSignatureAllowlisted(PackageInfo packageInfo) {
        Signature[] signatures = packageInfo.signatures;
        if (signatures.length != 1) {
            Log.w(TAG, "Package has more than one signature.");
            return false;
        }
        return isCertAllowlisted(
                packageInfo.packageName, signatures[0].toByteArray(), Build.IS_DEBUGGABLE);
    }

    private static boolean isCertAllowlisted(String packageName, byte[] cert, boolean isDebug) {
        try {
            byte[] certDigest = MessageDigest.getInstance("SHA-256").digest(cert);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Checking cert for " + (isDebug ? "debug" : "release"));
            }
            return Arrays.equals(certDigest, getDigestBytes(packageName, isDebug));
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException("Failed to obtain SHA-256 digest impl.", e);
        }
    }

    private static boolean verifyAllowlistedPackage(String packageName) {
        if (PACKAGE_GSA.equals(packageName)
                || PACKAGE_GMSCORE.equals(packageName)
                || PACKAGE_TIPS.equals(packageName)
                || PACKAGE_LAUNCHER.equals(packageName)
                || PACKAGE_PIXEL_TROUBLESHOOTING.equals(packageName)) {
            return true;
        }
        boolean isDebuggable = Build.IS_DEBUGGABLE;
        if (isDebuggable && PACKAGE_ASSISTANT_DEV_APP.equals(packageName)) {
            return true;
        }
        if (isDebuggable && PACKAGE_SETTINGS_API_TESTER.equals(packageName)) {
            return true;
        }
        return isDebuggable && PACKAGE_ROUTER_TEST_APP.equals(packageName);
    }

    private static byte[] getDigestBytes(String packageName, boolean isDebug) {
        switch (packageName) {
            case PACKAGE_ROUTER_TEST_APP:
                return ROUTER_TEST_APP;
            case PACKAGE_ASSISTANT_DEV_APP:
                return DEBUG_DIGEST_ASSISTANT_DEV_APP;
            case PACKAGE_TIPS:
                return isDebug ? DEBUG_DIGEST_TIPS : RELEASE_DIGEST_TIPS;
            case PACKAGE_LAUNCHER:
                return isDebug ? DEBUG_DIGEST_LAUNCHER : RELEASE_DIGEST_LAUNCHER;
            case PACKAGE_PIXEL_TROUBLESHOOTING:
                return isDebug
                        ? DEBUG_DIGEST_PIXEL_TROUBLESHOOTING
                        : RELEASE_DIGEST_PIXEL_TROUBLESHOOTING;
            default:
                return isDebug ? DEBUG_DIGEST_GMSCORE : RELEASE_DIGEST_GMSCORE;
        }
    }
}
