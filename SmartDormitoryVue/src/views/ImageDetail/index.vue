<template>
  <div>
    <div class="dashboard-container">
      <el-card style="width: 800px;">
        <div style="top: 10px;">实时监控 SN: {{ SNID }}</div>
        <img :src="'http://192.168.11.137:8003/video/' + SNID"
             class="video-image">
      </el-card>
      <el-card style="">
        <p style="height: 500px;width: 500px; white-space: pre-line;">{{ msg }}</p>
      </el-card>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import axios from 'axios'

export default {
  name: 'ImageDetail',
  computed: {},
  data() {
    return {
      SNID: '',
      msg: ""
    }
  },
  methods: {
    getMsg() {
      axios.get('http://192.168.11.137:8089/' + this.SNID)// 发送GET请求到指定的后端API接口
        .then(response => {
          // 处理响应数据
          // console.log(this.SNID)
          // console.log(response.data)
          this.msg = response.data
          // 创建一个新的 Date 对象，它将自动设置为当前时间
          var currentDate = new Date();

          // 获取年份、月份、日期、小时、分钟、秒数
          var year = currentDate.getFullYear();
          var month = currentDate.getMonth() + 1; // 月份是从 0 开始的，所以需要加 1
          var day = currentDate.getDate();
          var hour = currentDate.getHours();
          var minute = currentDate.getMinutes();
          var second = currentDate.getSeconds();

          // 格式化输出当前时间
          var formattedTime = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second;
          this.msg += "\n当前系统时间：" + formattedTime + "\n"
          // console.log("当前系统时间：" + formattedTime);
          // 获取浏览器信息
          var browserInfo = {
            userAgent: navigator.userAgent,
            appName: navigator.appName,
            appVersion: navigator.appVersion,
            platform: navigator.platform,
            language: navigator.language
          };
          // 使用模板字符串
          var infoString = `
          操作系统信息：
          User Agent: ${browserInfo.userAgent}
          App Name: ${browserInfo.appName}
          App Version: ${browserInfo.appVersion}
          Platform: ${browserInfo.platform}
          Language: ${browserInfo.language}
          `;

          this.msg += "\n"
          this.msg += infoString
          this.msg += "\n"
        })
        .catch(error => {
          // 处理错误
          console.error(error)
        })
    }
  },
  mounted() {
    this.SNID = this.$route.params.id
    this.getMsg()
    setInterval(this.getMsg, 1000);
  }
}
</script>

<style lang="scss" scoped>
.dashboard {
  &-container {
    margin: 30px;
  }

  &-text {
    font-size: 30px;
    line-height: 46px;
  }
}

.dashboard-container {
  display: flex;
  flex-direction: row;
}

.video-image {
  height: 100%;
  width: 100%;
  object-fit: fill; /* 可以根据需要使用 cover 或者其他值 */
}
</style>
