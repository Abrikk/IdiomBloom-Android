# 词库数据来源与许可

本项目的 v4 词库由以下内容合并、清洗和重新标注生成：

1. **THUOCL：清华大学开放中文词库**
   - 项目：https://github.com/thunlp/THUOCL
   - 用途：依据成语词频选择常用内容并排序。
   - 许可：MIT License；项目说明允许研究与商业使用，并要求在相关成果中声明使用了清华大学开放中文词库。
   - 建议引用：Shiyi Han, Yuhui Zhang, Yunshan Ma, Cunchao Tu, Zhipeng Guo, Zhiyuan Liu, Maosong Sun. THUOCL: Tsinghua Open Chinese Lexicon. 2016.

2. **chinese-xinhua**
   - 项目：https://github.com/pwxcoo/chinese-xinhua
   - 用途：提供基础成语、拼音、释义和例句数据。
   - 许可：MIT License。
   - 说明：上游项目注明数据由网络收集整理。正式商业发布前，应进一步核对具体内容和例句的准确性与授权情况。

3. **成语日课人工整理内容**
   - 原有示例词条、易错分类、易错提示、缺失词条及部分例句由本项目重新编写。

生成脚本为 `tools/build_dictionary.py`，易错专题数据为 `tools/error_prone_notes.json`。
