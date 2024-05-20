<template>
  <div>
    <div class="dashboard-container">
      <el-card v-if="cards.length === 0" class="card-item-none">
        <div class="bold-center">当前没有设备在线</div>
        <div class="bold-center">······</div>
        <div class="bold-center">请检查设备状况或者在设备插入后刷新页面</div>
      </el-card>
      <el-card v-for="card in cards" :key="card" class="card-item">
        <router-link :to="'/image/' + card">
          <div>SN：{{ card }}</div>
          <img
            :src="'http://192.168.11.137:8003/video/' + card"
            controls
            style="height: 90%; width: 100%;"
            alt="视频流获取失败，请检查设备摄像头状态..."
          >
        </router-link>
      </el-card>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'MasterPanel',
  data() {
    return {
      cards: []
    }
  },
  computed: {},
  mounted() {
    this.getDList()

  },
  methods: {
    getDList() {
      axios.get('http://192.168.11.137:8089/all') // 发送GET请求到指定的后端API接口
        .then(response => {
          // 处理响应数据
          const data = response.data
          // 将字符串根据 "-" 分割成数组
          this.cards = data.split("-")
          this.cards.pop()
          console.log(this.cards)
          console.log(data)

        })
        .catch(error => {
          // 处理错误
          console.error(error)
        })
    }

  }
}
</script>

<style lang="scss" scoped>
.dashboard-container {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-around;
}

.card-item {
  height: 250px;
  width: 400px;
  margin-bottom: 20px;
  box-shadow: 0px 0px 10px rgba(0, 0, 0, 0.3);

  &-none {
    height: 250px;
    width: 500px;
    margin-bottom: 40px;
    box-shadow: 0px 0px 10px rgba(0, 0, 0, 0.3);
  }
}

.bold-center {
  font-weight: bold;
  font-size: 22px;
  margin: 0px auto 40px auto;
  text-align: center;
}
</style>
