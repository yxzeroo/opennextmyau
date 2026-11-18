package myau.data;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;

public class EntityBox {
    public final EntityLivingBase entity;
    public final AxisAlignedBB boundingBox;
    public final double x;
    public final double y;
    public final double z;
    public final float yaw;
    public final float pitch;
    public final double distance;
    public final float fov;
    public final boolean visible;

    public EntityBox(
        EntityLivingBase entity,
        AxisAlignedBB boundingBox,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        double distance,
        float fov,
        boolean visible
    ) {
        this.entity = entity;
        this.boundingBox = boundingBox;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.distance = distance;
        this.fov = fov;
        this.visible = visible;
    }
}
