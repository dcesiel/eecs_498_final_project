/* LCM type definition class file
 * This file was automatically generated by lcm-gen
 * DO NOT MODIFY BY HAND!!!!
 */

package lcmtypes;
 
import java.io.*;
import java.util.*;
import lcm.lcm.*;
 
public final class gun_t implements lcm.lcm.LCMEncodable
{
    public long timestamp;
    public boolean fire;
 
    public gun_t()
    {
    }
 
    public static final long LCM_FINGERPRINT;
    public static final long LCM_FINGERPRINT_BASE = 0x22d96830b6ebd974L;
 
    static {
        LCM_FINGERPRINT = _hashRecursive(new ArrayList<Class<?>>());
    }
 
    public static long _hashRecursive(ArrayList<Class<?>> classes)
    {
        if (classes.contains(lcmtypes.gun_t.class))
            return 0L;
 
        classes.add(lcmtypes.gun_t.class);
        long hash = LCM_FINGERPRINT_BASE
            ;
        classes.remove(classes.size() - 1);
        return (hash<<1) + ((hash>>63)&1);
    }
 
    public void encode(DataOutput outs) throws IOException
    {
        outs.writeLong(LCM_FINGERPRINT);
        _encodeRecursive(outs);
    }
 
    public void _encodeRecursive(DataOutput outs) throws IOException
    {
        outs.writeLong(this.timestamp); 
 
        outs.writeByte( this.fire ? 1 : 0); 
 
    }
 
    public gun_t(byte[] data) throws IOException
    {
        this(new LCMDataInputStream(data));
    }
 
    public gun_t(DataInput ins) throws IOException
    {
        if (ins.readLong() != LCM_FINGERPRINT)
            throw new IOException("LCM Decode error: bad fingerprint");
 
        _decodeRecursive(ins);
    }
 
    public static lcmtypes.gun_t _decodeRecursiveFactory(DataInput ins) throws IOException
    {
        lcmtypes.gun_t o = new lcmtypes.gun_t();
        o._decodeRecursive(ins);
        return o;
    }
 
    public void _decodeRecursive(DataInput ins) throws IOException
    {
        this.timestamp = ins.readLong();
 
        this.fire = ins.readByte()!=0;
 
    }
 
    public lcmtypes.gun_t copy()
    {
        lcmtypes.gun_t outobj = new lcmtypes.gun_t();
        outobj.timestamp = this.timestamp;
 
        outobj.fire = this.fire;
 
        return outobj;
    }
 
}

