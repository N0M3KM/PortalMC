package dev.portalmod.assets;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Original procedural rigging. Joint adjustments operate in model space and include descendants. */
public final class ArmaturePose {
    private ArmaturePose() { }
    public static Matrix4f matrix(float[] a) {
        return new Matrix4f(a[0], a[4], a[8], 0, a[1], a[5], a[9], 0,
            a[2], a[6], a[10], 0, a[3], a[7], a[11], 1);
    }
    public static int bone(SourceModel model, String name) {
        for (int i = 0; i < model.bones().length; i++) if (model.bones()[i].name().equals(name)) return i;
        return -1;
    }
    public static Matrix4f[] bind(SourceModel model) {
        Matrix4f[] result = new Matrix4f[model.bones().length];
        for (int i = 0; i < result.length; i++) result[i] = matrix(model.bones()[i].inverseBind()).invert();
        return result;
    }
    public static Matrix4f[] skin(SourceModel model, Matrix4f[] posed) {
        Matrix4f[] result = new Matrix4f[posed.length];
        for (int i = 0; i < result.length; i++) result[i] = new Matrix4f(posed[i]).mul(matrix(model.bones()[i].inverseBind()));
        return result;
    }
    public static void rotate(SourceModel model, Matrix4f[] pose, String name, Quaternionf rotation) {
        int joint = bone(model, name);
        if (joint < 0) return;
        Vector3f pivot = pose[joint].getTranslation(new Vector3f());
        Matrix4f delta = new Matrix4f().translation(pivot).rotate(rotation).translate(-pivot.x, -pivot.y, -pivot.z);
        for (int i = 0; i < pose.length; i++) {
            int parent = i;
            while (parent >= 0 && parent != joint) parent = model.bones()[parent].parent();
            if (parent == joint) pose[i] = new Matrix4f(delta).mul(pose[i]);
        }
    }
    public static void translate(SourceModel model,Matrix4f[] pose,String name,Vector3f offset) {
        int joint=bone(model,name); if(joint<0) return;
        Matrix4f delta=new Matrix4f().translation(offset);
        for(int i=0;i<pose.length;i++) {
            int parent=i; while(parent>=0 && parent!=joint) parent=model.bones()[parent].parent();
            if(parent==joint) pose[i]=new Matrix4f(delta).mul(pose[i]);
        }
    }
    /** Two-bone IK keeps segment lengths; an out-of-reach grip is clamped instead of stretching. */
    public static void arm(SourceModel model, Matrix4f[] pose, String side, Vector3f target, Vector3f pole) {
        limb(model,pose,"bicep_"+side,"elbow_"+side,"wrist_"+side,target,pole);
    }
    public static void limb(SourceModel model, Matrix4f[] pose, String upperName, String middleName, String endName, Vector3f target, Vector3f pole) {
        int upper = bone(model, upperName), elbow = bone(model, middleName), wrist = bone(model, endName);
        if (upper < 0 || elbow < 0 || wrist < 0) return;
        Vector3f shoulder = pose[upper].getTranslation(new Vector3f());
        Vector3f elbowPos = pose[elbow].getTranslation(new Vector3f());
        Vector3f hand = pose[wrist].getTranslation(new Vector3f());
        Vector3f[] solution = solve(shoulder, elbowPos.distance(shoulder), hand.distance(elbowPos), target, pole);
        rotate(model, pose, upperName, new Quaternionf().rotationTo(elbowPos.sub(shoulder).normalize(), new Vector3f(solution[0]).sub(shoulder).normalize()));
        elbowPos = pose[elbow].getTranslation(new Vector3f()); hand = pose[wrist].getTranslation(new Vector3f());
        rotate(model, pose, middleName, new Quaternionf().rotationTo(hand.sub(elbowPos).normalize(), new Vector3f(solution[1]).sub(elbowPos).normalize()));
    }
    public static void upright(SourceModel model, Matrix4f[] pose, Matrix4f[] bind, String name) {
        int i=bone(model,name); if(i<0) return;
        Quaternionf delta=bind[i].getUnnormalizedRotation(new Quaternionf()).mul(pose[i].getUnnormalizedRotation(new Quaternionf()).conjugate());
        rotate(model,pose,name,delta);
    }
    public static Vector3f[] solve(Vector3f shoulder, float upper, float lower, Vector3f target, Vector3f pole) {
        Vector3f direction = new Vector3f(target).sub(shoulder);
        float distance = Math.clamp(direction.length(), Math.abs(upper - lower) + 0.001f, upper + lower - 0.001f);
        if (direction.lengthSquared() < 1e-8f) direction.set(0, 0, 1); else direction.normalize();
        Vector3f bend = new Vector3f(pole).sub(new Vector3f(direction).mul(pole.dot(direction)));
        if (bend.lengthSquared() < 1e-8f) bend = new Vector3f(direction).cross(Math.abs(direction.y) < 0.9f ? new Vector3f(0,1,0) : new Vector3f(1,0,0));
        bend.normalize();
        float along = (upper * upper + distance * distance - lower * lower) / (2 * distance);
        float height = (float)Math.sqrt(Math.max(0, upper * upper - along * along));
        return new Vector3f[]{new Vector3f(shoulder).fma(along, direction).fma(height, bend), new Vector3f(shoulder).fma(distance, direction)};
    }
}
