<template>
  <div>
    <div class="dashboard-container">
      <el-card style="height: 500px;width: 800px;">
        <div style="top: 10px;">实时监控</div>
        <img src="http://127.0.0.1:8003/video/E05A1BD2D9B4" controls style="height: 90%; width: 100%;">
      </el-card>
      <el-card style="height: 500px;flex: 1;">
        <div style="height: 500px; width: 100%;" ref="echarts"></div>
      </el-card>
    </div>
  </div>
  <!-- 右中:折线图 -->
</template>

<script>
import * as echarts from 'echarts'
import axios from 'axios'

export default {
  name: 'Dashboard',
  computed: {},
  data() {
    return {
      e: null
    }
  },
  methods: {
    // 折线图
    mmw_xy_chart() {
      var option = {
        title: {
          text: '人员坐标'
        },
        tooltip: {},

        xAxis: {
          type: 'value',
          min: -2.5,
          max: 2.5
        },
        yAxis: {
          type: 'value',
          min: 0,
          max: 6
        },
        series: [
          {
            name: '检测人员',
            type: 'scatter',
            data: [
              [-2, 4],
              [0, 3],
              [1, 5]
            ]
          }
        ]
      }

      this.e.setOption(option)
    },

    get_mmwdata() {
      axios.get('http://127.0.0.1:8000/get_t') // 发送GET请求到指定的后端API接口
        .then(response => {
          // 处理响应数据
          console.log(response.data)
          var option = {
            series: [
              {
                name: '检测人员',
                type: 'scatter',
                data: response.data
              }
            ]
          }
          this.e.setOption(option)
        })
        .catch(error => {
          // 处理错误
          console.error(error)
        })
    }

  },
  mounted() {
    this.e = echarts.init(this.$refs.echarts)
    this.mmw_xy_chart()
    // setInterval( this.get_mmwdata, 1000);
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
</style>
