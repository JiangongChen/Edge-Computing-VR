using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System;
using System.Runtime.InteropServices;

public class encodeD3D_copy2D : MonoBehaviour
{
    RenderTexture rTex;
    public new Camera camera;
    public Material _mat;

    [DllImport("AppEncD3D11", CallingConvention = CallingConvention.Cdecl)]
    private static extern int encD3D(System.IntPtr texture);

    [DllImport("AppEncD3D11", CallingConvention = CallingConvention.Cdecl)]
    private static extern int Encode(System.IntPtr texture);

    [DllImport("AppEncD3D11", CallingConvention = CallingConvention.Cdecl)]
    private static extern int resetEnc();

    bool enc = true;
    int nFrame=0;
    int totalFrameNum = 300;

    Texture2D toTexture2D(RenderTexture rTex)
    {
        rTex = new RenderTexture(640, 360, 24, RenderTextureFormat.BGRA32);
        camera.targetTexture = rTex;
        RenderTexture.active = rTex;                       
        camera.Render();
        Texture2D tex = new Texture2D(640, 360, TextureFormat.BGRA32, false);
        RenderTexture.active = rTex;
        //tex.ReadPixels(new Rect(0, 0, rTex.width, rTex.height), 0, 0);
        //tex.Apply();
        //tex.Apply(false);
        Graphics.CopyTexture(rTex, tex);
        return tex;
    }

    // Start is called before the first frame update
    void Start()
    {
        
    }

    // Update is called once per frame
    void Update()
    {
        if (enc)
        {
            //_mat.mainTexture = toTexture2D(rTex);
            Texture2D tex = toTexture2D(rTex);
            //System.IO.File.WriteAllBytes("D:/UnityDemo/NativePlugin/build_result/test.jpg", tex.EncodeToJPG());
            Debug.Log(encD3D(tex.GetNativeTexturePtr()));
            //Debug.Log(Encode(tex.GetNativeTexturePtr()));
            enc = false;
        }
        
        if (nFrame <= totalFrameNum)
        {
            this.transform.Translate(Vector3.forward);
            Texture2D tex = toTexture2D(rTex);
            Debug.Log("encoded frame number:"+Encode(tex.GetNativeTexturePtr()));
        }
        if (nFrame == totalFrameNum+1)
        {

            Debug.Log(resetEnc());
        }
        nFrame++;
    }
}
