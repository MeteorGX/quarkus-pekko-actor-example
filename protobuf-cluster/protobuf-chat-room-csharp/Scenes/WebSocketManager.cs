using Godot;
using Google.Protobuf;
using ProtobufChatRoomCsharp.Generated.Protobuf;

namespace ProtobufChatRoomCsharp.Scenes;

public partial class WebSocketManager : Node
{
    [Export] public string[] SupportedProtocols;

    [Export] public string[] HandshakeHeaders;


    [Signal]
    public delegate void ConnectedEventHandler();

    [Signal]
    public delegate void DisconnectedEventHandler();

    [Signal]
    public delegate void ReceivedEventHandler(Variant message);


    public WebSocketPeer Socket { get; private set; } = new();

    public WebSocketPeer.State LastState { get; private set; } = WebSocketPeer.State.Closed;

    private TlsOptions Options { get; set; }

    public Error ConnectToUrl(string url)
    {
        Socket.SupportedProtocols = SupportedProtocols;
        Socket.HandshakeHeaders = HandshakeHeaders;

        var err = Socket.ConnectToUrl(url, Options);
        if (err == Error.Ok)
        {
            return err;
        }

        LastState = Socket.GetReadyState();
        return Error.Ok;
    }


    public void Disconnect(int code = 1000, string reason = "")
    {
        Socket.Close(code, reason);
        LastState = Socket.GetReadyState();
    }

    public void Clear()
    {
        Socket = new WebSocketPeer();
        LastState = Socket.GetReadyState();
    }

    public Error SendTextMessage(string message)
    {
        return Socket.SendText(message);
    }

    public Error SendMessage(byte[] message, WebSocketPeer.WriteMode mode)
    {
        return Socket.Send(message, mode);
    }

    public Error SendCommand(int command, byte[] message)
    {
        var packed = new BytesMessage
        {
            Id = command,
            Message = ByteString.CopyFrom(message)
        };
        
        var stream = new MemoryStream();
        packed.WriteTo(stream);
        return Socket.Send(stream.ToArray());
    }


    private Variant GetMessage()
    {
        if (Socket.GetAvailablePacketCount() < 1)
        {
            return default;
        }

        var pkt = Socket.GetPacket();
        return Socket.WasStringPacket() ? pkt.GetStringFromUtf8() : GD.BytesToVar(pkt);
    }

    private void Poll()
    {
        if (Socket.GetReadyState() != WebSocketPeer.State.Closed)
        {
            Socket.Poll();
        }

        var state = Socket.GetReadyState();
        if (LastState != state)
        {
            LastState = state;
            switch (state)
            {
                case WebSocketPeer.State.Open:
                    EmitSignal(SignalName.Connected);
                    break;
                case WebSocketPeer.State.Closed:
                    EmitSignal(SignalName.Disconnected);
                    break;
                default:
                    // ignore
                    break;
            }
        }

        while (Socket.GetReadyState() == WebSocketPeer.State.Open && Socket.GetAvailablePacketCount() > 0)
        {
            EmitSignal(SignalName.Received, GetMessage());
        }
    }

    public override void _Process(double delta)
    {
        Poll();
    }
}