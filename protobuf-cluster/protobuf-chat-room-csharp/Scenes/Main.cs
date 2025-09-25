using System.Diagnostics;
using Godot;

namespace ProtobufChatRoomCsharp.Scenes;

public partial class Main : Control
{
    [Export] public Button ConnectButton { get; set; }
    [Export] public Button SendButton { get; set; }

    [Export] public LineEdit Hostname { set; get; }

    [Export] public LineEdit Message { set; get; }

    [Export] public RichTextLabel Body { set; get; }

    [Export] public WebSocketManager Manager { set; get; }

    /// <summary>
    /// initialize
    /// </summary>
    public override void _EnterTree()
    {
        Debug.Assert(Hostname != null);
        Debug.Assert(Message != null);
        Debug.Assert(Body != null);
        Debug.Assert(Manager != null);

        ConnectButton?.Connect(BaseButton.SignalName.Toggled, Callable.From<bool>(OnConnectButtonToggled));
        SendButton?.Connect(BaseButton.SignalName.Pressed, Callable.From(OnSendButtonPressed));
        Manager.Connected += OnConnected;
        Manager.Disconnected += OnDisconnect;
        Manager.Received += OnReceived;
    }


    private void OnConnected()
    {
        WriteMessage($"Client just connected with protocol: {Manager.Socket.GetSelectedProtocol()}");
        ConnectButton.Disabled = false;
        ConnectButton.Text = "Connect";
    }

    private void OnDisconnect()
    {
        var socket = Manager.Socket;
        WriteMessage($"Client just disconnected with code: {socket.GetCloseCode()}, reason: {socket.GetCloseReason()}");
        ConnectButton.Disabled = false;
        ConnectButton.Text = "Connect";
    }


    private void OnReceived(Variant message)
    {
        WriteMessage($"{message.AsString()}");
    }


    /// <summary>
    /// print message
    /// </summary>
    /// <param name="message">server message</param>
    private void WriteMessage(string message)
    {
        GD.Print(message);
        Body.AddText(message + System.Environment.NewLine);
    }


    /// <summary>
    /// connect server
    /// </summary>
    /// <param name="pressed"></param>
    private void OnConnectButtonToggled(bool pressed)
    {
        if (!pressed)
        {
            Manager.Disconnect();
            return;
        }

        if (Hostname.Text.Trim().Length == 0)
        {
            return;
        }

        ConnectButton.Disabled = true;
        ConnectButton.Text = "Connecting";
        WriteMessage($"Connecting to host: {Hostname.Text.Trim()}");
        var err = Manager.ConnectToUrl(Hostname.Text.Trim());
        if (err != Error.Ok)
        {
            WriteMessage($"Error connecting to host: {Hostname.Text.Trim()}");
        }
    }

    private void OnSendButtonPressed()
    {
        if (Message.Text.Trim().Length <= 0 || Manager.LastState != WebSocketPeer.State.Open) return;
        WriteMessage($"Sending message: {Message.Text}");
        //Manager.SendTextMessage(Message.Text);
        Manager.SendCommand(100, Message.Text.ToUtf8Buffer());
        Message.Text = "";
    }
}