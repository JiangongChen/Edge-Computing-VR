using System.Collections;
using System.Collections.Generic;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Linq;
using System.Threading;
using UnityEngine;
using System;
using System.IO;
using System.Runtime.InteropServices;

public class encodeServer : MonoBehaviour
{
    /// <summary> 	
    /// TCPListener to listen for incomming TCP connection 	
    /// requests. 	
    /// </summary> 	
    private TcpListener tcpListener;
    /// <summary> 
    /// Background thread for TcpServer workload. 	
    /// </summary> 	
    private Thread tcpListenerThread;
    /// <summary> 	
    /// Create handle to connected tcp client. 	
    /// </summary> 	
    private TcpClient connectedTcpClient;

    //private Socket clientSocket; // We will only accept one socket.
    private static readonly Socket serverSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
    private static readonly List<Socket> clientSockets = new List<Socket>();
    private const int PORT = 8080;
    private const int BUFFER_SIZE = 64;
    private static readonly byte[] buffer = new byte[BUFFER_SIZE];
    private static byte[][] dataBuf;
    private static bool recv_flag;
    private static bool pos_flag;
    private static int tranFrame = 0;

    RenderTexture rTex;
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
    int nFrame = 0;
    static int totalFrameNum = 300;


    // Start is called before the first frame update
    void Start()
    {
        rTex = new RenderTexture(640, 360, 24, RenderTextureFormat.BGRA32);
        camera.targetTexture = rTex;

        dataBuf = new byte[totalFrameNum+1][];

        // Start TcpServer background thread 		
        tcpListenerThread = new Thread(new ThreadStart(SetupServer));
        tcpListenerThread.IsBackground = true;
        tcpListenerThread.Start();
        recv_flag = false;
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
            IntPtr pkg = Encode(rTex.GetNativeTexturePtr(), ref pkg_size);
            Debug.Log(pkg_size);
            if (pkg_size != 0)
            {
                byte[] data = new byte[pkg_size];
                Marshal.Copy(pkg, data, 0, pkg_size);
                dataBuf[nFrame] = data;
            }

        }
        if (nFrame == totalFrameNum + 1)
        {
            Debug.Log(resetEnc());
        }
        nFrame++;
    }

    /// <summary>
    /// Construct server socket and bind socket to all local network interfaces, then listen for connections
    /// with a backlog of 10. Which means there can only be 10 pending connections lined up in the TCP stack
    /// at a time. This does not mean the server can handle only 10 connections. The we begin accepting connections.
    /// Meaning if there are connections queued, then we should process them.
    /// </summary>
    private static void SetupServer()
    {
        try
        {
            Debug.Log("Setting up server...");
            serverSocket.Bind(new IPEndPoint(IPAddress.Any, PORT));
            //serverSocket.Bind(new IPEndPoint(IPAddress.Parse("192.168.0.190"), PORT));
            serverSocket.Listen(0);
            serverSocket.BeginAccept(AcceptCallback, null);
            Debug.Log("Server setup complete");
        }
        catch (SocketException ex)
        {
            Debug.Log("SocketException " + ex.ToString());
        }
        catch (ObjectDisposedException ex)
        {
            Debug.Log("SocketException " + ex.ToString());
        }

    }

    private static void AcceptCallback(IAsyncResult AR)
    {
        Socket socket;

        try
        {
            socket = serverSocket.EndAccept(AR);
        }
        catch (ObjectDisposedException) // I cannot seem to avoid this (on exit when properly closing sockets)
        {
            return;
        }

        clientSockets.Add(socket);
        socket.BeginReceive(buffer, 0, BUFFER_SIZE, SocketFlags.None, ReceiveCallback, socket);
        string clientIP = ((IPEndPoint)socket.RemoteEndPoint).Address.ToString();
        Debug.Log("Connection successfully established, from: " + clientIP + ", waiting for request...");
        serverSocket.BeginAccept(AcceptCallback, null);
    }
    private static void ReceiveCallback(IAsyncResult AR)
    {
        Socket current = (Socket)AR.AsyncState;
        int received;
        Byte[] bytes = new Byte[32];
        Byte[] size_bytes = new Byte[32];
        Byte[] byte_buff = new Byte[10240];
        int length;

        try
        {
            received = current.EndReceive(AR);
            //Debug.Log(received);
        }
        catch (SocketException)
        {
            Debug.Log("Client forcefully disconnected");
            tranFrame = 0;
            // Don't shutdown because the socket may be disposed and its disconnected anyway.
            current.Close();
            clientSockets.Remove(current);
            return;
        }

        byte[] recBuf = new byte[received];
        Array.Copy(buffer, recBuf, received);
        string text = Encoding.ASCII.GetString(recBuf);

        if (tranFrame > totalFrameNum)
        {
            Debug.Log("All frames have been transmitted, client closed.");
            tranFrame = 0;
            current.Close();
            clientSockets.Remove(current);
            return;
        }
        //Debug.Log("beginning: " + text);
        recv_flag = true;
        byte[] whole_image = null;
        while (dataBuf[tranFrame] == null)
        {
            tranFrame++;
        }
        whole_image = dataBuf[tranFrame];
        int size = whole_image.Length;
        Debug.Log("send frame: " + tranFrame);
        tranFrame++;
        string size_string = size.ToString();
        //Debug.Log("image size: " + size);
        // Convert string message to byte array.                 
        size_bytes = Encoding.ASCII.GetBytes(size_string);
        // Write byte array to socketConnection stream.               
        current.Send(size_bytes);
        //Debug.Log("Send the message of image size - should be received by client");

        // Read incomming stream into byte arrary. 		
        length = current.Receive(bytes);
        var incommingData = new byte[length];
        Array.Copy(bytes, 0, incommingData, 0, length);
        // Convert byte array to string message. 							
        string clientMessage = Encoding.ASCII.GetString(incommingData);
        //Debug.Log("shake hands: " + clientMessage);

        int send_size = 0;
        while (send_size < size)
        {
            if (send_size + byte_buff.Length < size)
            {
                Array.Copy(whole_image, send_size, byte_buff, 0, byte_buff.Length);
                current.Send(byte_buff);
            }
            else
            {
                Array.Copy(whole_image, send_size, byte_buff, 0, size - send_size);
                current.Send(byte_buff, size - send_size, 0);
            }

            send_size += byte_buff.Length;
        }
        //Debug.Log("Current frame has been sent. Ready for next frame.");

        current.BeginReceive(buffer, 0, BUFFER_SIZE, SocketFlags.None, ReceiveCallback, current);
    }

    private void OnDestroy()
    {
        tcpListenerThread.Abort();
        Debug.Log("Background thread has been closed.");
    }

}
