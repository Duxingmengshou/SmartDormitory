#include <unistd.h>
#include <iostream>
#include <iomanip>
#include <string>
#include <vector>
#include "opencv2/imgproc/imgproc.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "tengine_c_api.h"
#include <sys/time.h>
#include "common.hpp"
#include "opencv2/opencv.hpp"
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <cstring>

#define DEF_PROTO "models/MobileNetSSD_deploy.prototxt"
#define DEF_MODEL "models/MobileNetSSD_deploy.caffemodel"

struct Box
{
    float x0;
    float y0;
    float x1;
    float y1;
    int class_idx;
    float score;
};

void get_input_mat_ssd(cv::Mat &img, float *input_data, int img_h, int img_w)
{
    if (img.empty())
    {
        std::cerr << "img null"
                  << ".\n";
        return;
    }

    cv::resize(img, img, cv::Size(img_h, img_w));
    img.convertTo(img, CV_32FC3);
    float *img_data = (float *)img.data;
    int hw = img_h * img_w;

    float mean[3] = {127.5, 127.5, 127.5};
    for (int h = 0; h < img_h; h++)
    {
        for (int w = 0; w < img_w; w++)
        {
            for (int c = 0; c < 3; c++)
            {
                input_data[c * hw + h * img_w + w] = 0.007843 * (*img_data - mean[c]);
                img_data++;
            }
        }
    }
}

void post_process_ssd_up(cv::Mat &img, float threshold, float *outdata, int num,int client_socket_msg)
{
    const char *class_names[] = {"background", "aeroplane", "bicycle", "bird", "boat", "bottle",
                                 "bus", "car", "cat", "chair", "cow", "diningtable",
                                 "dog", "horse", "motorbike", "person", "pottedplant", "sheep",
                                 "sofa", "train", "tvmonitor"};
    int raw_h = img.size().height;
    int raw_w = img.size().width;
    std::vector<Box> boxes;
    int line_width = raw_w * 0.005;
    printf("detect result num: %d \n", num);
    int pnum=0;
    char msg[1024];
    int offset = 0;
    offset+=sprintf(msg+offset,"检测信息：\n");
    for (int i = 0; i < num; i++)
    {
        if (outdata[1] >= threshold&&outdata[0]==15)
        {
            Box box;
            box.class_idx = outdata[0];
            box.score = outdata[1];
            box.x0 = outdata[2] * raw_w;
            box.y0 = outdata[3] * raw_h;
            box.x1 = outdata[4] * raw_w;
            box.y1 = outdata[5] * raw_h;
            boxes.push_back(box);
            pnum++;
            // 第一个 sprintf
            offset += sprintf(msg + offset, "置信率：%s\t:%.0f%%\n", class_names[box.class_idx], box.score * 100);
            // 第二个 sprintf，注意偏移量的使用
            //offset += sprintf(msg + offset, "BOX:( %g , %g ),( %g , %g )\n", box.x0, box.y0, box.x1, box.y1);
            // 打印消息
            printf(msg);
        }
        outdata += 6;
    }
    offset += sprintf(msg + offset, "寝室人数：%d 人\n", pnum);
    send(client_socket_msg, msg, strlen(msg), 0); 
    for (int i = 0; i < (int)boxes.size(); i++)
    {
        Box box = boxes[i];
        cv::rectangle(img, cv::Rect(box.x0, box.y0, (box.x1 - box.x0), (box.y1 - box.y0)), cv::Scalar(255, 255, 0),
                      line_width);
        std::ostringstream score_str;
        score_str << box.score;
        std::string label = std::string(class_names[box.class_idx]) + ": " + score_str.str();
        int baseLine = 0;
        cv::Size label_size = cv::getTextSize(label, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);
        cv::rectangle(img,
                      cv::Rect(cv::Point(box.x0, box.y0 - label_size.height),
                               cv::Size(label_size.width, label_size.height + baseLine)),
                      cv::Scalar(255, 255, 0), CV_FILLED);
        cv::putText(img, label, cv::Point(box.x0, box.y0), cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(0, 0, 0));
    }
}

int main(int argc, char *argv[])
{
    int ret = -1;
    const std::string root_path = get_root_path();
    std::string proto_file;
    std::string model_file;
    const char *device = nullptr;
    if (proto_file.empty())
    {
        proto_file = root_path + DEF_PROTO;
        std::cout << "proto file not specified,using " << proto_file << " by default\n";
    }
    if (model_file.empty())
    {
        model_file = root_path + DEF_MODEL;
        std::cout << "model file not specified,using " << model_file << " by default\n";
    }
    // init tengine
    if (init_tengine() < 0)
    {
        std::cout << " init tengine failed\n";
        return 1;
    }
    if (request_tengine_version("0.9") != 1)
    {
        std::cout << " request tengine version failed\n";
        return 1;
    }
    // check file
    if (!check_file_exist(proto_file) or !check_file_exist(model_file))
    {
        return 1;
    }
    // create graph
    graph_t graph = create_graph(nullptr, "caffe", proto_file.c_str(), model_file.c_str());
    if (graph == nullptr)
    {
        std::cout << "Create graph failed\n";
        std::cout << " ,errno: " << get_tengine_errno() << "\n";
        return 1;
    }
    if (device != nullptr)
    {
        set_graph_device(graph, device);
    }
    // dump_graph(graph);
    // input
    int img_h = 300;
    int img_w = 300;
    int img_size = img_h * img_w * 3;
    float *input_data = (float *)malloc(sizeof(float) * img_size);
    int node_idx = 0;
    int tensor_idx = 0;
    tensor_t input_tensor = get_graph_input_tensor(graph, node_idx, tensor_idx);
    if (input_tensor == nullptr)
    {
        std::printf("Cannot find input tensor,node_idx: %d,tensor_idx: %d\n", node_idx, tensor_idx);
        return -1;
    }
    int dims[] = {1, 3, img_h, img_w};
    set_tensor_shape(input_tensor, dims, 4);
    ret = prerun_graph(graph);
    if (ret != 0)
    {
        std::cout << "Prerun graph failed, errno: " << get_tengine_errno() << "\n";
        return 1;
    }
    int repeat_count = 1;
    const char *repeat = std::getenv("REPEAT_COUNT");
    if (repeat)
        repeat_count = std::strtoul(repeat, NULL, 10);

    // 创建 socket 客户端
    int client_socket = socket(AF_INET, SOCK_STREAM, 0);
    struct sockaddr_in server_address;
    const char *host_addr = "192.168.11.137";
    server_address.sin_family = AF_INET;
    server_address.sin_port = htons(8004);
    inet_pton(AF_INET, host_addr, &server_address.sin_addr);

    // 连接到服务器
    if (connect(client_socket, (struct sockaddr *)&server_address, sizeof(server_address)) < 0)
    {
        std::cerr << "Error: Unable to connect to the server." << std::endl;
        return -1;
    }

    // 发送 SN 码，用于唯一标识客户端
    const char *sn_code = "4E67E4F83443";
    send(client_socket, sn_code, strlen(sn_code), 0);

    // socket传递图像分析信息
    int client_socket_msg = socket(AF_INET, SOCK_STREAM, 0);
    struct sockaddr_in server_address_msg;
    const char *host_addr_msg = "192.168.11.137";
    server_address_msg.sin_family = AF_INET;
    server_address_msg.sin_port = htons(8083);
    inet_pton(AF_INET, host_addr_msg, &server_address_msg.sin_addr);

    // 连接服务器
    if (connect(client_socket_msg, (struct sockaddr *)&server_address_msg, sizeof(server_address_msg)) < 0)
    {
        std::cerr << "Error: Unable to connect to the server2." << std::endl;
        return -1;
    }

    const char *sn_code_msg="4E67E4F83443";
    send(client_socket_msg, sn_code_msg, strlen(sn_code_msg), 0);

    // 从摄像头读取视频流
    cv::VideoCapture cap(0);

    // 检查摄像头是否成功打开
    if (!cap.isOpened())
    {
        std::cerr << "Error: Unable to open camera." << std::endl;
        close(client_socket);
        return -1;
    }

    // 逐帧读取视频并发送到服务器
    cv::Mat img;
    cv::TickMeter tm;
    int n = 1;
    while (true)
    {
        tm.start();
        cap.read(img);
        if (img.empty())
        {
            std::cerr << "Error: Failed to capture frame." << std::endl;
            break;
        }

        get_input_mat_ssd(img, input_data, img_h, img_w); //得到input
        set_tensor_buffer(input_tensor, input_data, img_size * 4);
        ret = run_graph(graph, 1);
        if (ret != 0)
        {
            std::cout << "Run graph failed, errno: " << get_tengine_errno() << "\n";
            return 1;
        }

        tensor_t out_tensor = get_graph_output_tensor(graph, 0, 0);
        int out_dim[4];
        ret = get_tensor_shape(out_tensor, out_dim, 4);
        if (ret <= 0)
        {
            std::cout << "get tensor shape failed, errno: " << get_tengine_errno() << "\n";
            return 1;
        }
        float *outdata = (float *)get_tensor_buffer(out_tensor);
        int num = out_dim[1];
        float show_threshold = 0.5;

        post_process_ssd_up(img, show_threshold, outdata, num, client_socket_msg); //绘制结果

        // 将帧转换为二进制流数据
        std::vector<uchar> buffer;
        cv::imencode(".jpeg", img, buffer);
        int buffer_size = buffer.size();
        // send(client_socket, &buffer_size, sizeof(buffer_size), 0);
        send(client_socket, buffer.data(), buffer_size, 0);
        send(client_socket, "\x6A\x70\x65\x67\x0A", 5, 0);

        tm.stop();
        double fps = n++ / tm.getTimeSec();
        std::cout << "FPS: " << fps << std::endl;
        release_graph_tensor(out_tensor);
        release_graph_tensor(input_tensor);
    }

    // 关闭连接
    close(client_socket);
    cap.release();
    cv::destroyAllWindows();
    // 关闭模型
    ret = postrun_graph(graph);
    if (ret != 0)
    {
        std::cout << "Postrun graph failed, errno: " << get_tengine_errno() << "\n";
        return 1;
    }
    free(input_data);
    destroy_graph(graph);
    release_tengine();
    return 0;
}

