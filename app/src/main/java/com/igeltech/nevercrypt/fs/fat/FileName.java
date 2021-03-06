package com.igeltech.nevercrypt.fs.fat;

import com.igeltech.nevercrypt.fs.util.StringPathUtil;

import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

class FileName
{
    static private final CharsetEncoder asciiEncoder = StandardCharsets.US_ASCII.newEncoder(); // or
    private final String _name;
    public boolean isLFN;
    public boolean isLowerCaseName;
    public boolean isLowerCaseExtension;

    public FileName(String name)
    {
        _name = name;
        init();
    }

    public static boolean isLegalDosChar(char c)
    {
        if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c >= 128 && c <= 255))
            return true;
        for (byte b : DirEntry.ALLOWED_SYMBOLS)
            if (b == c)
                return true;
        return false;
        // Arrays.asList(DirEntry.ALLOWED_SYMBOLS). .contains(c);
    }

    public static boolean isLegalDosName(String fn)
    {
        boolean space = false;
        for (int i = 0, l = fn.length(); i < l; i++)
        {
            char c = fn.charAt(i);
            if (c == ' ')
            {
                if (space)
                    return false;
                space = true;
            }
            else if (!isLegalDosChar(c))
                return false;
        }
        return true;
    }

    public static boolean isPureAscii(String v)
    {
        return asciiEncoder.canEncode(v);
    }

    public static String toUpperCase(String s)
    {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            if (c >= 'a' || c <= 'z')
                res.append(Character.toUpperCase(c));
            else
                res.append(c);
        }
        return res.toString();
    }

    public static String toLowerCase(String s)
    {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            if (c >= 'a' || c <= 'z')
                res.append(Character.toLowerCase(c));
            else
                res.append(c);
        }
        return res.toString();
    }

    public String getDosName(int counter)
    {
        if (_name.equals(".") || _name.equals(".."))
            return extendName(_name, 11);
        StringPathUtil p = new StringPathUtil(_name);
        String fn = p.getFileNameWithoutExtension().toUpperCase();
        String filteredName = "";
        for (int i = 0, l = fn.length(); i < l; i++)
        {
            char c = fn.charAt(i);
            if (c == ' ')
                break;
            if (!isLegalDosChar(c))
                c = '~';
            filteredName += c;
        }
        if (counter > 0)
        {
            String counterString = String.valueOf(counter);
            if (counterString.length() >= 8)
                filteredName = counterString.substring(0, 8);
            else
                filteredName = filteredName.substring(0, Math.min(8, filteredName.length()) - counterString.length()) + counterString;
        }
        fn = extendName(filteredName, 8);
        String ex = p.getFileExtension().toUpperCase();
        filteredName = "";
        for (int i = 0, l = ex.length(); i < l; i++)
        {
            char c = ex.charAt(i);
            if (c == ' ')
                break;
            if (!isLegalDosChar(c))
                c = '~';
            filteredName += c;
        }
        ex = extendName(filteredName, 3);
        return fn + ex;
    }

    private void init()
    {
        isLFN = isLowerCaseExtension = isLowerCaseName = false;
        if (_name.equals(".") || _name.equals(".."))
            return;
        StringPathUtil p = new StringPathUtil(_name);
        String fn = p.getFileNameWithoutExtension();
        if (fn.equals(toUpperCase(fn)))
            isLowerCaseName = false;
        else if (fn.equals(toLowerCase(fn)))
            isLowerCaseName = true;
        else
            isLFN = true;
        String ex = p.getFileExtension();
        if (ex.equals(toUpperCase(ex)))
            isLowerCaseExtension = false;
        else if (ex.equals(toLowerCase(ex)))
            isLowerCaseExtension = true;
        else
            isLFN = true;
        //DEBUG
        //Log.d("EDS",String.format("%s.%s lcn=%s lce=%s lfn=%s", fn,ex,isLowerCaseName,isLowerCaseExtension,isLFN));
        if (!isLFN && (fn.length() > 8 || ex.length() > 3 || !isLegalDosName(fn) || !isPureAscii(_name)))
            isLFN = true;
    }

    private String extendName(String name, int targetLen)
    {
        if (name.length() > targetLen)
            return name.substring(0, targetLen - 1) + '~';
        for (int i = name.length(); i < targetLen; i++)
            name += ' ';
        return name;
    }
    // "ISO-8859-1"
    // for
    // ISO
    // Latin
    // 1
}