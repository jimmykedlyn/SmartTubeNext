package com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.V2;

import android.content.Context;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.DefaultAudioSink;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.V2.framedrop.AmlogicFix2MediaCodecVideoRenderer;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.V2.framedrop.CompoundFixMediaCodecVideoRenderer;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.V2.framedrop.FrameDropFixMediaCodecVideoRenderer;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;

import java.util.ArrayList;

/**
 * Main intent: override audio delay
 */
public class CustomOverridesRenderersFactory extends DefaultRenderersFactory {
    private static final String TAG = CustomOverridesRenderersFactory.class.getSimpleName();
    private static final String[] FRAME_DROP_FIX_LIST = {
            "T95ZPLUS (q201_3GB)",
            "UGOOS (UGOOS)",
            "55UC30G (ctl_iptv_mrvl)" // Kivi 55uc30g
    };
    private final PlayerData mPlayerData;

    public CustomOverridesRenderersFactory(FragmentActivity activity) {
        super(activity);
        setExtensionRendererMode(EXTENSION_RENDERER_MODE_ON);
        setEnableDecoderFallback(true);
        //setMediaCodecSelector(new BlackListMediaCodecSelector());

        mPlayerData = PlayerData.instance(activity);
    }

    /**
     * Delay audio<br/>
     * All real delay happens in {@link AudioDelayMediaCodecAudioRenderer}
     */
    @Override
    protected void buildAudioRenderers(
            Context context,
            @ExtensionRendererMode int extensionRendererMode,
            MediaCodecSelector mediaCodecSelector,
            @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
            boolean playClearSamplesWithoutKeys,
            boolean enableDecoderFallback,
            AudioProcessor[] audioProcessors,
            Handler eventHandler,
            AudioRendererEventListener eventListener,
            ArrayList<Renderer> out) {
        super.buildAudioRenderers(
                context,
                extensionRendererMode,
                mediaCodecSelector,
                drmSessionManager,
                playClearSamplesWithoutKeys,
                enableDecoderFallback,
                audioProcessors,
                eventHandler,
                eventListener,
                out);

        Renderer audioRenderer = null;

        if (mPlayerData.getAudioDelayMs() != 0) {
            AudioDelayMediaCodecAudioRenderer audioDelayRenderer =
                    new AudioDelayMediaCodecAudioRenderer(
                            context,
                            mediaCodecSelector,
                            drmSessionManager,
                            playClearSamplesWithoutKeys,
                            enableDecoderFallback,
                            eventHandler,
                            eventListener,
                            new DefaultAudioSink(AudioCapabilities.getCapabilities(context), audioProcessors));
            audioDelayRenderer.setAudioDelayMs(mPlayerData.getAudioDelayMs());

            audioRenderer = audioDelayRenderer;
        }

        if (audioRenderer != null) {
            Renderer originMediaCodecAudioRenderer = null;
            int index = 0;

            for (Renderer renderer : out) {
                if (renderer instanceof MediaCodecAudioRenderer) {
                    originMediaCodecAudioRenderer = renderer;
                    break;
                }
                index++;
            }

            if (originMediaCodecAudioRenderer != null) {
                // replace origin with custom
                out.remove(originMediaCodecAudioRenderer);
                out.add(index, audioRenderer);
            }
        }
    }

    @Override
    protected void buildVideoRenderers(Context context,
                                       int extensionRendererMode,
                                       MediaCodecSelector mediaCodecSelector,
                                       @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
                                       boolean playClearSamplesWithoutKeys,
                                       boolean enableDecoderFallback,
                                       Handler eventHandler,
                                       VideoRendererEventListener eventListener,
                                       long allowedVideoJoiningTimeMs,
                                       ArrayList<Renderer> out) {
        super.buildVideoRenderers(
                context,
                extensionRendererMode,
                mediaCodecSelector,
                drmSessionManager,
                playClearSamplesWithoutKeys,
                enableDecoderFallback,
                eventHandler,
                eventListener,
                allowedVideoJoiningTimeMs,
                out);

        Renderer videoRenderer = null;

        if (mPlayerData.isFrameDropFixEnabled() && mPlayerData.isAmlogicFixEnabled()) {
            videoRenderer = new CompoundFixMediaCodecVideoRenderer(
                    context,
                    mediaCodecSelector,
                    allowedVideoJoiningTimeMs,
                    drmSessionManager,
                    playClearSamplesWithoutKeys,
                    enableDecoderFallback,
                    eventHandler,
                    eventListener,
                    MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY);
        } else if (mPlayerData.isFrameDropFixEnabled()) {
            videoRenderer = new FrameDropFixMediaCodecVideoRenderer(
                    context,
                    mediaCodecSelector,
                    allowedVideoJoiningTimeMs,
                    drmSessionManager,
                    playClearSamplesWithoutKeys,
                    enableDecoderFallback,
                    eventHandler,
                    eventListener,
                    MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY);
        } else if (mPlayerData.isAmlogicFixEnabled()) {
            videoRenderer = new AmlogicFix2MediaCodecVideoRenderer(
                    context,
                    mediaCodecSelector,
                    allowedVideoJoiningTimeMs,
                    drmSessionManager,
                    playClearSamplesWithoutKeys,
                    enableDecoderFallback,
                    eventHandler,
                    eventListener,
                    MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY);
        }

        if (videoRenderer != null) {
            Renderer originMediaCodecVideoRenderer = null;
            int index = 0;

            for (Renderer renderer : out) {
                if (renderer instanceof MediaCodecVideoRenderer) {
                    originMediaCodecVideoRenderer = renderer;
                    break;
                }
                index++;
            }

            if (originMediaCodecVideoRenderer != null) {
                // replace origin with custom
                out.remove(originMediaCodecVideoRenderer);
                out.add(index, videoRenderer);
            }
        }
    }
}
