package dev.portalmod.client.visual;

import dev.portalmod.config.ConfigManager;
import java.nio.file.Files;
import net.fabricmc.loader.api.FabricLoader;

/** New visual constants are estimates, independently authored and expressed in blocks/degrees/seconds. */
public final class PortalVisualConfig {
    public boolean armPose = true, thirdPersonGun = true;
    public int visualRevision = 2;
    public boolean portalCrosshair = true;
    public int crosshairSize = 23, crosshairBlue = 0xff28aaff, crosshairOrange = 0xffff8a19;
    public float gunGlassOpacity = .65f;
    public float footStrideUnits = 7, footLiftUnits = 4, crouchStanceUnits = 2, airFootLiftUnits = 7, jumpFootForwardUnits = 5;
    public float gripX = -9, gripY = 55, gripZ = 17, supportGripX = 3, supportGripY = 55, supportGripZ = 15;
    public float armPoleOut = 1, armPoleDown = -1, armPoleForward = 0;
    public float aimPitchLimit = 65, headPitchLimit = 60, headYawLimit = 65;
    public float handRadius = 0.045f, forearmRadius = 0.052f;
    public float viewArmX = 0.52f, viewArmY = -0.65f, viewArmZ = 0.08f;
    public float originalGunLength = 0.65f, originalGunRadius = 0.12f;
    public boolean characterAnimation = true, originalCharacterFallback = true;
    public boolean authoredCharacterAnimations = true;
    public int maxDecodedAnimationMegabytes = 64;
    public float authoredMovingBlendSpeed = .5f, authoredUnitsPerBlock = 40;
    public boolean idleWeightShift = true, strideAnimation = true, strafeLean = true, accelerationTilt = true;
    public boolean crouchPose = true, jumpPose = true, airbornePose = true, landingSquash = true, headFollow = true, characterRecoil = true;
    public float poseResponse = 12, aimResponse = 16, strideRadiansPerBlock = 3.5f, runSpeed = 8;
    public float idleShiftDegrees = 1.5f, idleFrequency = 1.1f, strideDegrees = 32, kneeDegrees = 38;
    public float strafeLeanDegrees = 7, accelerationTiltDegrees = 6, accelerationScale = 20;
    public float crouchDropUnits = 8, crouchBendDegrees = 25, jumpBendDegrees = 20, airborneBendDegrees = 12;
    public float landingMaxDropUnits = 5, landingBendDegrees = 22, landingReferenceSpeed = 15, landingRecovery = .3f;
    public float takeoffRecovery = .18f, characterRecoilDegrees = 5, characterRecoilRecovery = .18f;
    public boolean gunRecoil = true, gunProngs = true, gunGlow = true, gunIdleSway = true, gunMovementBob = true, gunFizzleAnimation = true;
    public float fireAttack = .025f, fireRecovery = .16f, fireKickDegrees = 6;
    public float prongOpenDegrees = 12, emitterTravel = .025f, glowPulse = .25f;
    public float bobAmplitude = .012f, bobFrequency = 9, swayFrequency = 1.6f;
    public float fizzleDuration = .3f, fizzleShakeDegrees = 3, fizzleFrequency = 35;
    public float emitterRadius = .055f, emitterTipOffset = .025f;
    public boolean cameraEffects = true, landingViewPunch = true, fireViewPunch = true, exitViewPunch = true, reduceMotion = false;
    public float motionIntensity = 1, landingPunchDegrees = 5, landingPunchSeconds = .32f;
    public float firePunchDegrees = 1.2f, firePunchSeconds = .16f, exitPunchDegrees = .7f, exitRollDegrees = .35f, exitPunchSeconds = .22f, exitReferenceSpeed = 25;
    public boolean hudEnabled = false, hudDebug = true, hudMonospace = true, hudShadow = true, hudBackground = false;
    public int hudMargin = 6, hudLineHeight = 10, hudColor = -1;
    public static PortalVisualConfig current = new PortalVisualConfig();
    public static void load() {
        var path = FabricLoader.getInstance().getConfigDir().resolve("portalmod-visuals.json");
        try {
            PortalVisualConfig c = Files.exists(path) ? ConfigManager.JSON.fromJson(Files.readString(path), PortalVisualConfig.class) : new PortalVisualConfig();
            if (c == null) throw new IllegalArgumentException("Missing visual settings");
            if (Files.exists(path)) {
                var json=com.google.gson.JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                if(!json.has("visualRevision")) { if(c.supportGripX==-2) c.supportGripX=3; if(c.supportGripZ==20) c.supportGripZ=15; c.visualRevision=2; }
            }
            for (var field : PortalVisualConfig.class.getFields()) if (field.getType() == float.class && !Float.isFinite(field.getFloat(c))) throw new IllegalArgumentException("Non-finite " + field.getName());
            c.gunGlassOpacity=Math.clamp(c.gunGlassOpacity,0,1); c.crosshairSize=Math.clamp(c.crosshairSize,15,49);
            c.maxDecodedAnimationMegabytes=Math.clamp(c.maxDecodedAnimationMegabytes,1,256);
            c.authoredMovingBlendSpeed=Math.clamp(c.authoredMovingBlendSpeed,.1f,8); c.authoredUnitsPerBlock=Math.clamp(c.authoredUnitsPerBlock,1,100);
            c.footStrideUnits=Math.clamp(c.footStrideUnits,0,12); c.footLiftUnits=Math.clamp(c.footLiftUnits,0,10);
            c.crouchStanceUnits=Math.clamp(c.crouchStanceUnits,0,5); c.airFootLiftUnits=Math.clamp(c.airFootLiftUnits,0,15); c.jumpFootForwardUnits=Math.clamp(c.jumpFootForwardUnits,0,10);
            c.aimPitchLimit = Math.clamp(c.aimPitchLimit, 0, 75); c.headPitchLimit = Math.clamp(c.headPitchLimit, 0, 75); c.headYawLimit = Math.clamp(c.headYawLimit, 0, 85);
            c.handRadius = Math.clamp(c.handRadius, .01f, .08f); c.forearmRadius = Math.clamp(c.forearmRadius, .01f, .1f);
            c.originalGunLength = Math.clamp(c.originalGunLength, .2f, 1); c.originalGunRadius = Math.clamp(c.originalGunRadius, .04f, .2f);
            c.poseResponse = Math.clamp(c.poseResponse, 1, 40); c.aimResponse = Math.clamp(c.aimResponse, 1, 40);
            c.runSpeed = Math.clamp(c.runSpeed, 1, 30); c.accelerationScale = Math.clamp(c.accelerationScale, 1, 100);
            c.landingReferenceSpeed = Math.clamp(c.landingReferenceSpeed, 1, 100);
            c.landingRecovery = Math.clamp(c.landingRecovery, .05f, 2); c.takeoffRecovery = Math.clamp(c.takeoffRecovery, .05f, 2);
            c.characterRecoilRecovery = Math.clamp(c.characterRecoilRecovery, .05f, 2);
            c.fireAttack = Math.clamp(c.fireAttack, .005f, .2f); c.fireRecovery = Math.clamp(c.fireRecovery, .05f, 1);
            c.fizzleDuration = Math.clamp(c.fizzleDuration, .05f, 2); c.bobAmplitude = Math.clamp(c.bobAmplitude, 0, .1f);
            c.prongOpenDegrees = Math.clamp(c.prongOpenDegrees, 0, 45); c.emitterRadius = Math.clamp(c.emitterRadius, .01f, .15f);
            c.motionIntensity = Math.clamp(c.motionIntensity,0,2);
            c.landingPunchSeconds = Math.clamp(c.landingPunchSeconds,.05f,2); c.firePunchSeconds = Math.clamp(c.firePunchSeconds,.05f,2); c.exitPunchSeconds = Math.clamp(c.exitPunchSeconds,.05f,2);
            c.exitReferenceSpeed = Math.clamp(c.exitReferenceSpeed,1,100);
            c.strideRadiansPerBlock=Math.clamp(c.strideRadiansPerBlock,.1f,20); c.glowPulse=Math.clamp(c.glowPulse,0,1);
            c.bobFrequency=Math.clamp(c.bobFrequency,0,30); c.swayFrequency=Math.clamp(c.swayFrequency,0,10);
            c.hudLineHeight=Math.clamp(c.hudLineHeight,8,20); c.hudMargin=Math.clamp(c.hudMargin,0,100);
            current = c;
            Files.createDirectories(path.getParent()); Files.writeString(path, ConfigManager.JSON.toJson(c));
        } catch (Exception e) { throw new IllegalStateException("Cannot load " + path, e); }
    }
}
