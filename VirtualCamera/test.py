from time import sleep

import cv2
import socket
import random


def generate_sn():
    sn = ""
    for _ in range(12):
        sn += random.choice("0123456789ABCDEF")
    return sn


# sn = generate_sn()
sn='12345678'
print(sn)

# 建立socket客户端
host_addr = 'localhost'
client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_address = (host_addr, 8083)
client_socket.connect(server_address)

# 发送sn码，用于唯一标识客户端
client_socket.sendall(sn.encode())

# 读取MP4文件
video_path = './1-p.jpg'
cap = cv2.VideoCapture(0)





# 发送二进制流数据到服务器
while True:
    # 将帧转换为二进制流数据
    ret, frame = cap.read()
    _, binary_data = cv2.imencode('.jpg', frame)
    binary_data = binary_data.tobytes()
    binary_data += '\x6A\x70\x65\x67\x0A'.encode()
    client_socket.sendall(binary_data)


# 关闭连接
client_socket.close()
