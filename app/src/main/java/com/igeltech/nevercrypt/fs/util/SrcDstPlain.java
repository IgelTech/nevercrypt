package com.igeltech.nevercrypt.fs.util;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.igeltech.nevercrypt.android.Logger;
import com.igeltech.nevercrypt.fs.Path;
import com.igeltech.nevercrypt.locations.Location;
import com.igeltech.nevercrypt.locations.LocationsManager;
import com.igeltech.nevercrypt.locations.LocationsManagerBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class SrcDstPlain extends ArrayList<SrcDstCollection.SrcDst> implements SrcDstCollection
{
    public static final Parcelable.Creator<SrcDstPlain> CREATOR = new Parcelable.Creator<SrcDstPlain>()
    {
        public SrcDstPlain createFromParcel(Parcel in)
        {
            SrcDstPlain res = new SrcDstPlain();
            try
            {
                LocationsManager lm = LocationsManagerBase.getLocationsManager(null, false);
                int size = in.readInt();
                for (int i = 0; i < size; i++)
                {
                    Uri u = in.readParcelable(getClass().getClassLoader());
                    Location srcLoc = lm != null ? lm.getLocation(u) : null;
                    u = in.readParcelable(getClass().getClassLoader());
                    Location dstLoc = Uri.EMPTY.equals(u) ? null : lm != null ? lm.getLocation(u) : null;
                    if (srcLoc != null)
                        res.add(srcLoc, dstLoc);
                }
            }
            catch (Exception e)
            {
                Logger.log(e);
            }
            return res;
        }

        public SrcDstPlain[] newArray(int size)
        {
            return new SrcDstPlain[size];
        }
    };
    private static final long serialVersionUID = 1L;

    public static SrcDstCollection fromPaths(Location srcLoc, Location dstLoc, Collection<? extends Path> srcPaths)
    {
        if (srcPaths == null)
            return new SrcDstSingle(srcLoc, dstLoc);
        SrcDstPlain res = new SrcDstPlain();
        for (Path p : srcPaths)
        {
            Location l = srcLoc.copy();
            l.setCurrentPath(p);
            res.add(l, dstLoc);
        }
        return res;
    }

    public void add(Location srcLoc, Location dstLoc)
    {
        super.add(new SrcDstSimple(srcLoc, dstLoc));
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        int size = size();
        dest.writeInt(size);
        for (int i = 0; i < size; i++)
        {
            SrcDst c = get(i);
            try
            {
                dest.writeParcelable(c.getSrcLocation().getLocationUri(), flags);
            }
            catch (IOException e)
            {
                dest.writeParcelable(Uri.EMPTY, flags);
            }
            try
            {
                dest.writeParcelable(c.getDstLocation() == null ? Uri.EMPTY : c.getDstLocation().getLocationUri(), flags);
            }
            catch (IOException e)
            {
                dest.writeParcelable(Uri.EMPTY, flags);
            }
        }
    }

    private static class SrcDstSimple implements SrcDst
    {
        private final Location _srcLoc, _dstLoc;

        public SrcDstSimple(Location srcLoc, Location dstLoc)
        {
            _srcLoc = srcLoc;
            _dstLoc = dstLoc;
        }

        @Override
        public Location getSrcLocation()
        {
            return _srcLoc;
        }

        @Override
        public Location getDstLocation()
        {
            return _dstLoc;
        }
    }
}
