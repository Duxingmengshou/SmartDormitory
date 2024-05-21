from time import sleep

import cv2
import socket
import random

def generate_sn():
    sn = ""
    for _ in range(12):
        sn += random.choice("0123456789ABCDEF")
    return sn

sn = generate_sn()
print(sn)

# 建立socket客户端
host_addr = 'localhost'
client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_address = (host_addr, 8004)
client_socket.connect(server_address)

client_socket_mmw = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_address_mmw = (host_addr, 8083)
client_socket_mmw.connect(server_address_mmw)

# 发送sn码，用于唯一标识客户端
client_socket.sendall(sn.encode())
client_socket_mmw.sendall(sn.encode())

# 读取MP4文件
video_path = './1-p.jpg'
cap = cv2.VideoCapture(video_path)
ret, frame = cap.read()
# 逐帧读取视频并发送到服务器
while cap.isOpened():
    # ret, frame = cap.read()
    if not ret:
        break

    # 将帧转换为二进制流数据
    _, binary_data = cv2.imencode('.jpg', frame)
    binary_data = binary_data.tobytes()
    binary_data += '\x6A\x70\x65\x67\x0A'.encode()

    # 发送二进制流数据到服务器
    client_socket.sendall(binary_data)
    sleep(0.02)
    mmw_data="测试设备，仅供测试\n"
    mmw_data=mmw_data.encode()
    client_socket_mmw.sendall(mmw_data)
# 关闭连接
client_socket.close()
client_socket_mmw.close()