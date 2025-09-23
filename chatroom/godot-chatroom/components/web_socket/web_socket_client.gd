extends Control

@onready var _manager: WebSocketManager = $WebSocketManager
@onready var _hostname:LineEdit = $Panel/VBoxContainer/Menu/Hostname
@onready var _connect:Button = $Panel/VBoxContainer/Menu/Connect
@onready var _log_desc: RichTextLabel = $Panel/VBoxContainer/Body
@onready var _content:LineEdit = $Panel/VBoxContainer/Command/Content


func info(msg):
	print(msg)
	_log_desc.add_text(str(msg) + "\n")


# ============================
# UI Signals
# ============================

# connect websocket
func _on_connect_toggled(toggled_on: bool) -> void:
	if not toggled_on:
		_manager.close()
		return
	if _hostname.text.is_empty():
		return
	info("Connecting to host: %s." % [_hostname.text])
	
	# connect
	_connect.text = "Connecting"
	_connect.disabled = true
	var err = _manager.connect_to_url(_hostname.text)
	if err != OK:
		info("Error connecting to host: %s" % [_hostname.text])
		return
	

# send text message
func _on_send_pressed() -> void:
	if _content.text.is_empty() or _manager.is_open():
		return 
	info("Sending message: %s" % [_content.text])
	_manager.send_message(_content.text)
	_content.text = ""


# ============================
# WebSocket Signals
# ============================

func _on_web_socket_manager_connected() -> void:
	info("Client just connected with protocol: %s" % _manager.get_socket().get_selected_protocol())
	_connect.text = "Connect"
	_connect.disabled = false


func _on_web_socket_manager_closed() -> void:
	var ws = _manager.get_socket()
	info("Client just disconnected with code: %s, reson: %s" % [ws.get_close_code(), ws.get_close_reason()])
	_connect.text = "Connect"
	_connect.disabled = false

func _on_web_socket_manager_received(message: Variant) -> void:
	info("%s" % message)
