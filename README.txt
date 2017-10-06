RER.java的main函数实例化一个RER类，参数分别是：
comDir：评论集所在文件夹(如，WeiBoMovie下放置了十四部微博电影评论)
nameDir：对应的演员角色人名文件夹
relaxtedDir：最终结果所在文件夹位置

99行calculate()->194行调用WebSearchName，参数是：
this.actor:存储这部电影的演员、角色名字的链表
url:构造的搜索链接字符串
unKnow:去搜的未知名字

WebSearchName.java 第54行设置每搜完一个名字RER.java的main函数实例化一个RER类，参数分别是：
comDir：评论集所在文件夹
nameDir：对应的演员角色人名文件夹
relaxtedDir：最终结果所在文件夹位置

99行calculate()->194行调用WebSearchName，参数是：
this.actor:存储这部电影的演员、角色名字的链表
url:构造的搜索链接字符串
unKnow:去搜的未知名字

WebSearchName.java 第54行设置每搜完一个名字后停顿的时间

## ！！哎～Python的代码忘记遵循Python的命名规范了，变量名不应该驼峰的～
<b>actor_emotion_score.py</b>是情感分析的代码，参数分别是：
comPath:放置有评论集合的文件夹
评论格式为：以电影名为文件名
"拆弹专家""4092092614178533",ISODate("2017-04-02T14:26:09.000+0800"),"不想割肉"NumberInt(10),"#拆弹专家#4月28日，华仔宋佳不见不散​",
namePath:放置演员角色名的文件夹。
以电影名为文件名，内容如：刘亦菲-白浅-司音-素素
emotionDir:所生成的结果存放的文件夹

<b>normalize.py</b>是对最后的结果做一个归一化处理
由actor_emotion_score.py得出来的情感分会很大，计算每一个演员的情感分占比




