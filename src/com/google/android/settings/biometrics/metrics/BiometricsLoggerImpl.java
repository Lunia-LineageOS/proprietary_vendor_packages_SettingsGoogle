package com.google.android.settings.biometrics.metrics;

import android.util.Log;
import android.util.proto.ProtoOutputStream;
import android.util.proto.ProtoStream;

import com.android.settings.biometrics.BiometricsOnboardingProto.FromSource;
import com.android.settings.biometrics.BiometricsOnboardingProto.Modality;
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingAction;
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingResult;
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingScreen;
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingScreenInfo;
import com.android.settings.biometrics.BiometricsOnboardingProto.RepeatedOnboardingScreenInfo;
import com.android.settings.biometrics.BiometricsOnboardingProto.RepeatedSettingsBiometricsOnboarding;
import com.android.settings.biometrics.BiometricsOnboardingProto.SettingsBiometricsOnboarding;
import com.android.settings.biometrics.metrics.BiometricsLogger;
import com.android.settings.biometrics.metrics.OnboardingEvent;
import com.android.settings.biometrics.metrics.OnboardingScreenInfoEvent;
import com.android.settings.core.instrumentation.SettingsStatsLog;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.List;

public class BiometricsLoggerImpl implements BiometricsLogger {
    private static final String TAG = "BiometricsLogger";

    // RepeatedOnboardingScreenInfo.info_list
    private static final long FIELD_INFO_LIST =
            ProtoStream.FIELD_COUNT_REPEATED | ProtoStream.FIELD_TYPE_MESSAGE | 1;
    // OnboardingScreenInfo.onboarding_screen
    private static final long FIELD_ONBOARDING_SCREEN =
            ProtoStream.FIELD_COUNT_SINGLE | ProtoStream.FIELD_TYPE_ENUM | 1;
    // OnboardingScreenInfo.onboarding_actions
    private static final long FIELD_ONBOARDING_ACTIONS =
            ProtoStream.FIELD_COUNT_REPEATED | ProtoStream.FIELD_TYPE_ENUM | 2;
    // OnboardingScreenInfo.dwell_time_millis
    private static final long FIELD_DWELL_TIME_MILLIS =
            ProtoStream.FIELD_COUNT_SINGLE | ProtoStream.FIELD_TYPE_INT64 | 3;

    @Override
    public void logSettingsBiometricsOnboarding(OnboardingEvent onboardingEvent) {
        ProtoOutputStream protoOutputStream = new ProtoOutputStream();
        for (OnboardingScreenInfoEvent screenInfo : onboardingEvent.getScreenInfos()) {
            addScreenInfosToProto(protoOutputStream, screenInfo);
        }
        SettingsStatsLog.write(
                SettingsStatsLog.SETTINGS_BIOMETRICS_ONBOARDING,
                onboardingEvent.getModality(),
                onboardingEvent.getFromSource(),
                onboardingEvent.getUserId(),
                onboardingEvent.getEnrolledCount(),
                onboardingEvent.getDuration(),
                onboardingEvent.getCapybaraStatus(),
                onboardingEvent.getResultCode(),
                onboardingEvent.getErrorCode(),
                protoOutputStream.getBytes());
    }

    private void addScreenInfosToProto(
            ProtoOutputStream protoOutputStream,
            OnboardingScreenInfoEvent onboardingScreenInfoEvent) {
        long token = protoOutputStream.start(FIELD_INFO_LIST);
        protoOutputStream.write(FIELD_ONBOARDING_SCREEN, onboardingScreenInfoEvent.getScreen());
        for (int action : onboardingScreenInfoEvent.getActions()) {
            protoOutputStream.write(FIELD_ONBOARDING_ACTIONS, action);
        }
        protoOutputStream.write(FIELD_DWELL_TIME_MILLIS, onboardingScreenInfoEvent.getDuration());
        protoOutputStream.end(token);
    }

    @Override
    public byte[] eventToMessageByteArray(OnboardingEvent onboardingEvent) {
        return eventToMessage(onboardingEvent).toByteArray();
    }

    private SettingsBiometricsOnboarding eventToMessage(OnboardingEvent onboardingEvent) {
        ArrayList<OnboardingScreenInfo> screenInfoList = new ArrayList<>();
        for (OnboardingScreenInfoEvent screenInfoEvent : onboardingEvent.getScreenInfos()) {
            ArrayList<OnboardingAction> actions = new ArrayList<>();
            for (int action : screenInfoEvent.getActions()) {
                actions.add(OnboardingAction.forNumber(action));
            }
            screenInfoList.add(
                    OnboardingScreenInfo.newBuilder()
                            .setOnboardingScreen(
                                    OnboardingScreen.forNumber(screenInfoEvent.getScreen()))
                            .setDwellTimeMillis(screenInfoEvent.getDuration())
                            .addAllOnboardingActions(actions)
                            .build());
        }
        return SettingsBiometricsOnboarding.newBuilder()
                .setModality(Modality.forNumber(onboardingEvent.getModality()))
                .setFromSource(FromSource.forNumber(onboardingEvent.getFromSource()))
                .setUser(onboardingEvent.getUserId())
                .setEnrolledCount(onboardingEvent.getEnrolledCount())
                .setDurationMillis(onboardingEvent.getDuration())
                .setCapybaraStatus(onboardingEvent.getCapybaraStatus())
                .setResultCode(OnboardingResult.forNumber(onboardingEvent.getResultCode()))
                .setErrorCode(onboardingEvent.getErrorCode())
                .setOnboardingScreenInfoList(
                        RepeatedOnboardingScreenInfo.newBuilder()
                                .addAllInfoList(screenInfoList)
                                .build())
                .build();
    }

    @Override
    public OnboardingEvent messageByteArrayToEvent(byte[] messageBytes) {
        if (messageBytes != null && messageBytes.length != 0) {
            try {
                return new OnboardingEvent(
                        SettingsBiometricsOnboarding.newBuilder().mergeFrom(messageBytes).build());
            } catch (InvalidProtocolBufferException e) {
                Log.w(TAG, e.getMessage());
            }
        }
        return null;
    }

    @Override
    public byte[] eventListToRepeatedMessageByteArray(List<OnboardingEvent> events) {
        ArrayList<SettingsBiometricsOnboarding> messages = new ArrayList<>();
        for (OnboardingEvent event : events) {
            messages.add(eventToMessage(event));
        }
        return RepeatedSettingsBiometricsOnboarding.newBuilder()
                .addAllRepeatedBiometricsOnboarding(messages)
                .build()
                .toByteArray();
    }
}
