extends Node
class_name WebSocketManager

@export var handshake_headers: PackedStringArray
@export var supported_protocols: PackedStringArray
var tls_options:TLSOptions = null

# =======================
# WebSocket Handler
# =======================
var socket: WebSocketPeer = WebSocketPeer.new()
var last_state: WebSocketPeer.State = WebSocketPeer.STATE_CLOSED


# =======================
# Event Signal
# =======================
signal connected()
signal closed()
signal received(message:Variant)


# connect to server
func connect_to_url(url:String)->int:
	socket.supported_protocols = supported_protocols
	socket.handshake_headers = handshake_headers
	var err = socket.connect_to_url(url,tls_options)
	if err != OK:
		return err
	last_state = socket.get_ready_state()
	return OK

# send message to server
func send_message(msg)->int:
	if typeof(msg) == TYPE_STRING:
		return socket.send_text(msg)
	return socket.send(var_to_bytes(msg))

# response message from server
func recv_message()->Variant:
	if socket.get_available_packet_count() < 0:
		return null
	var pkt = socket.get_packet()
	if socket.was_string_packet():
		return pkt.get_string_from_utf8()
	return bytes_to_var(pkt)

# close server
func close(code:=1000, reason:= "")->void:
	socket.close(code,reason)
	last_state = socket.get_ready_state()
	
# socket getter
func get_socket()->WebSocketPeer:
	return socket
	
	
func is_open()->bool:
	return last_state != socket.STATE_OPEN
	
# event each
func poll()->void:
	if socket.get_ready_state() != socket.STATE_CLOSED:
		socket.poll()
	var state = socket.get_ready_state()
	if last_state != state:
		last_state = state
		if state == socket.STATE_OPEN:
			connected.emit()
		elif state == socket.STATE_CLOSED:
			closed.emit()
	while socket.get_ready_state() == socket.STATE_OPEN and socket.get_available_packet_count():
		received.emit(recv_message())
	
func _process(_delta: float) -> void:
	poll()
