package framework.com.controllerandhead;

/**
 * Created by wangjf on 8/14/17.
 */

interface DataTransfer {
    public boolean trackControllerData(float x, float y, float z, float w);
    public boolean trackHeadData(float x, float y, float z, float w);
}
