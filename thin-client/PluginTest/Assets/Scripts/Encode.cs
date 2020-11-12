using System.Collections;
using System.Collections.Generic;
using System;
using System.Runtime.InteropServices;
using UnityEngine;

public class Encode : MonoBehaviour
{
    public RenderTexture rTex;

    [DllImport("AppEncCuda", CallingConvention = CallingConvention.Cdecl)]
    private static extern IntPtr EncodeImg();

    [DllImport("AppEncCuda", CallingConvention = CallingConvention.Cdecl)]
    private static extern IntPtr EncodeCuda(System.IntPtr texture);

    [DllImport("AppEncCuda", CallingConvention = CallingConvention.Cdecl)]
    private static extern IntPtr PrintHello();

    bool enc = true;

    Texture2D toTexture2D(RenderTexture rTex)
    {
        Texture2D tex = new Texture2D(640, 360, TextureFormat.ARGB32, false);
        RenderTexture.active = rTex;
        tex.Apply(false);
        Graphics.CopyTexture(rTex, tex);
        return tex;
    }

    // Start is called before the first frame update
    void Start()
    {
        Debug.Log(Marshal.PtrToStringAnsi(PrintHello()));
        Debug.Log(Marshal.PtrToStringAnsi(EncodeImg()));
    }

    // Update is called once per frame
    void Update()
    {
        if (enc)
        {
            Texture2D tex = toTexture2D(rTex);
            Debug.Log(Marshal.PtrToStringAnsi(EncodeCuda(tex.GetNativeTexturePtr())));
            enc = false;
        }
    }
}
