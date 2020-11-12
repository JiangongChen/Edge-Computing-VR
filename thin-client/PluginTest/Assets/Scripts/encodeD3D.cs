using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System;
using System.IO;
using System.Runtime.InteropServices;

public class encodeD3D : MonoBehaviour
{
    RenderTexture rTex;
    FileStream fs;
    public new Camera camera;
    public Material _mat;

    [DllImport("AppEncD3D11", CallingConvention = CallingConvention.Cdecl)]
    private static extern int encD3D(System.IntPtr texture);

    [DllImport("AppEncD3D11", CallingConvention = CallingConvention.Cdecl)]
    private static extern IntPtr Encode(System.IntPtr texture, ref int pkg_size);

    [DllImport("AppEncD3D11", CallingConvention = CallingConvention.Cdecl)]
    private static extern int resetEnc();

    [DllImport("AppEncD3D11", CallingConvention = CallingConvention.Cdecl)]
    private static extern int freeMem(IntPtr ptr);

    bool enc = true;
    int nFrame=0;
    int totalFrameNum = 300;


    // Start is called before the first frame update
    void Start()
    {
        rTex = new RenderTexture(640, 360, 24, RenderTextureFormat.BGRA32);
        camera.targetTexture = rTex;
        fs = File.Create("D:/UnityDemo/NativePlugin/build_result/out_unity.mp4");
        
    }

    // Update is called once per frame
    void Update()
    {
        if (enc)
        {
            Debug.Log(encD3D(rTex.GetNativeTexturePtr()));
            enc = false;
        }
        
        if (nFrame <= totalFrameNum)
        {
            this.transform.Translate(Vector3.forward);
            int pkg_size = 0;
            //Debug.Log("encoded frame number:"+Encode(rTex.GetNativeTexturePtr()));
            IntPtr pkg = Encode(rTex.GetNativeTexturePtr(), ref pkg_size);
            //byte[] managedArray = new byte[10240];
            //Marshal.Copy(pkg, managedArray, 0, 100);
            //Debug.Log(managedArray.Length);
            Debug.Log(pkg_size);
            if (pkg_size != 0)
            {
                byte[] data = new byte[pkg_size];
                Marshal.Copy(pkg, data, 0, pkg_size);
                fs.Write(data, 0, pkg_size);
            }
            
        }
        if (nFrame == totalFrameNum+1)
        {
            fs.Close();
            Debug.Log(resetEnc());
        }
        nFrame++;
    }
}
