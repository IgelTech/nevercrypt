package com.igeltech.nevercrypt.exceptions;

public class NativeError extends Exception
{
    public static final int ENOENT = -2;
    public static final int EIO = -5;
    public static final int EBADF = -9;
    private static final long serialVersionUID = 1L;
    public int errno;

    public NativeError(int errn)
    {
        errno = errn;
    }
}
