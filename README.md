# AudioBook (有声书项目)

Utilized **sherpa-onnx runtime** with **multi-threaded streaming** processing to achieve real-time and smooth audiobook playback on Android.

使用 **sherpa-onnx 推理引擎** 和 **多线程流式处理** 实现了 Android 平台下实时流畅的听书功能。

https://github.com/k2-fsa/sherpa-onnx

An Android-based audiobook application powered by sherpa-onnx TTS and VITS Melo, VITS COQUI models, supporting high-quality Chinese and English speech synthesis from text.

这是一个基于 Android 的听书应用，集成 sherpa-onnx TTS 和 VITS Melo，VITS COQUI 模型，支持高质量的中英文文本语音合成。

▶️ [Demo Video | 演示视频](https://www.youtube.com/watch?v=AaWBFhxBYSY)

---

## ✨ Features | 功能亮点

- 🔊 **Text-to-Speech (TTS)** using sherpa-onnx and VITS Melo for both English and Chinese  
  使用 sherpa-onnx 和 VITS Melo 实现中英文文本到语音的转换  
- 💡 **Streaming audio processing** for continuous and low-latency playback  
  流式语音处理实现连续、低延迟的播放体验  
- 📱 **Android-native interface** designed for a smooth and responsive user experience  
  Android 原生界面，优化流畅性与响应速度  

---

## 🚀 Getting Started | 快速开始

You can directly download the APK from the [Releases](https://github.com/DongYangYuWHJ/audioBook/releases) page.

你可以直接从 [Releases](https://github.com/DongYangYuWHJ/audioBook/releases) 页面下载 APK 文件。

Alternatively, build from source:

或者从源码构建：

### 1. Clone the repository | 克隆项目

```bash
git clone https://github.com/DongYangYuWHJ/audioBook.git
```

### 2. Open in Android Studio | 在 Android Studio 中打开

- Open the project using Android Studio
- Make sure all dependencies are resolved
- Use a real device or emulator to run the app  
- 使用 Android Studio 导入项目，确保依赖正确，运行在模拟器或真机上

---

## 🧠 Tech Stack | 技术栈

- **Android (Kotlin & Java)**  
- **Android UI**  
- **Multi-threading** for smooth audio streaming  
- **Sherpa-ONNX** for TTS engine  
- **VITS-Melo TTS models** (Chinese & English)

---

## 📄 License | 许可证

This project is licensed under the **MIT License**.  
本项目基于 **MIT 许可证** 开源。


目前开发功能：粘贴板：
# AudioNovel 应用改进计划

## 功能分析与建议

### 1. 文件管理优化
- 文件名验证与处理
    - 特殊字符检查
    - 空格处理
    - 重复名称处理
- 文件元信息显示
    - 最后修改时间
    - 文本长度/句子数量
    - 文件大小
- 文件操作增强
    - 重命名功能
    - 删除确认机制
    - 文件分类
    - 搜索功能
    - 排序选项（按名称、时间、大小等）

### 2. 状态管理
- 编辑状态保存
    - 切换底部导航时的状态保持
    - 应用退出时的自动保存
    - 未保存内容的处理
- 文件状态跟踪
  ```kotlin
  enum class FileState {
      EDITING,      // 编辑中
      SAVED,        // 已保存
      MODIFIED,     // 已修改
      LOADING       // 加载中
  }
  ```

### 3. 用户体验改进
#### 文本编辑
- 撤销/重做功能
- 文本查找
- 文本格式化
- 自动保存
- 输入长度控制

#### 界面优化
- 加载进度指示
- 操作成功/失败提示
- 文件列表优化
- 平滑的切换动画

### 4. 性能优化
- 大文本处理
    - 异步加载
    - 分页显示
    - 内存优化
- 文件操作
    - 异步读写
    - 缓存管理
    - 存储空间检查

### 5. 安全性
- 文件操作确认
    - 删除确认
    - 覆盖确认
    - 未保存内容提示
- 输入验证
    - 文件名验证
    - 文本长度限制
    - 存储空间检查

## 实现优先级
1. 基础功能
    - 底部导航栏
    - 粘贴板文本编辑
    - 基本文件操作
    - 文本朗读集成

2. 必要的安全特性
    - 删除确认
    - 自动保存
    - 基本输入验证

3. 核心体验优化
    - 状态管理
    - 进度指示
    - 操作反馈

4. 高级功能
    - 文本处理增强
    - 文件管理增强
    - 性能优化

## 技术注意事项
1. 使用 Visibility 控制底部导航栏，保持功能的互通性
2. 异步处理大文本操作
3. 实现可靠的状态保存机制
4. 优化内存使用
5. 确保平滑的用户体验