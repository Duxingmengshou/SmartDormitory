<!-- <template>
  <div class="app-container">
    <el-input v-model="filterText" placeholder="Filter keyword" style="margin-bottom:30px;" />

    <el-tree
      ref="tree2"
      :data="data2"
      :props="defaultProps"
      :filter-node-method="filterNode"
      class="filter-tree"
      default-expand-all
    />

  </div>
</template>

<script>
export default {

  data() {
    return {
      filterText: '',
      data2: [{
        id: 1,
        label: 'Level one 1',
        children: [{
          id: 4,
          label: 'Level two 1-1',
          children: [{
            id: 9,
            label: 'Level three 1-1-1'
          }, {
            id: 10,
            label: 'Level three 1-1-2'
          }]
        }]
      }, {
        id: 2,
        label: 'Level one 2',
        children: [{
          id: 5,
          label: 'Level two 2-1'
        }, {
          id: 6,
          label: 'Level two 2-2'
        }]
      }, {
        id: 3,
        label: 'Level one 3',
        children: [{
          id: 7,
          label: 'Level two 3-1'
        }, {
          id: 8,
          label: 'Level two 3-2'
        }]
      }],
      defaultProps: {
        children: 'children',
        label: 'label'
      }
    }
  },
  watch: {
    filterText(val) {
      this.$refs.tree2.filter(val)
    }
  },

  methods: {
    filterNode(value, data) {
      if (!value) return true
      return data.label.indexOf(value) !== -1
    }
  }
}
</script>
 -->



<template>
  <div>
    <div class="dashboard-container">
      <!-- <el-card style="height: 250px;width: 400px;">
        <div style="top: 10px;"></div>
        <img src="http://127.0.0.1:8003/video/E05A1BD2D9B4" controls style="height: 90%; width: 100%;">
      </el-card>
      <el-card style="height: 250px;width: 400px;">
        <div style="top: 10px;"></div>
        <img src="http://127.0.0.1:8003/video/E05A1BD2D9B4" controls style="height: 90%; width: 100%;">
      </el-card> -->

      <el-card v-for="card in cards" :key="card[0]" style="height: 250px;width: 400px;">
        <router-link :to="'/image/' + cards[0][0]">
          <img :src="'http://127.0.0.1:8003/video/' + card[0]" controls style="height: 90%; width: 100%;">
        </router-link>
      </el-card>
    </div>
  </div>
  <!-- 右中:折线图 -->
</template>

<script>
import axios from 'axios';

export default {
  computed: {

  },
  data() {
    return {
      cards: [],
    }
  },
  methods: {
    get_mmwdata() {
      axios.get('http://127.0.0.1:8000/get_sn_list')  // 发送GET请求到指定的后端API接口
        .then(response => {
          // 处理响应数据
          // console.log(response.data);
          this.cards = response.data;
          // console.log(this.cards[0][0])
          // console.log(this.cards[0][1])
        })
        .catch(error => {
          // 处理错误
          console.error(error);
        });
    },

  },
  mounted() {
    this.get_mmwdata();
    // setInterval( this.get_mmwdata, 1000);
  },
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
