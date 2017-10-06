#encoding:utf8

import re
from snownlp import SnowNLP
from snownlp import sentiment
import random
import sys
import os
reload(sys)
sys.setdefaultencoding('utf8')


class actor_emotion_score:

    '''
    actorIndex = []  # 演员：下标列表：[{吴京：0}，{张翰：1}……]
    nameIndex = []  # 名字：下标列表： [{吴京：0}，{京哥：0}，……]
    nameScore = []  # 名字：情感分列表：[{吴京：0.7},{京哥：0.5},……}
    emotionDict = []  # 情感列表：[{0:0}，{1:0}，{2:0}……]
    '''
    ''''''
    def __init__(self, namePath, comPath, resultPath):

        self.actorIndex = []  # 演员：下标列表：[{吴京：0}，{张翰：1}……]
        self.nameIndex = []  # 名字：下标列表： [{吴京：0}，{京哥：0}，……]
        self.nameScore = []  # 名字：情感分列表：[{吴京：0.7},{京哥：0.5},……}
        self.emotionDict = []  # 情感列表：[{0:0}，{1:0}，{2:0}……]

        self.read_file(namePath)  # 完善类变量
        # sentiment.train('/home/yingying/PycharmProjects/snowNLP/emotion_file/neg', '/home/yingying/PycharmProjects/snowNLP/emotion_file/pos')
        # sentiment.save('sentiment.marshal')
        self.actor_score(comPath)
        print "actorIndex"
        self.printList(self.actorIndex)
        print ""
        print "nameIndex"
        self.printList(self.nameIndex)
        print ""
        print "nameScore"
        self.printList(self.nameScore)
        print ""
        print "emotionDict"
        self.printList(self.emotionDict)
        print ""
        print ""
	# 打印输出结果：演员名：情感分
        for i in range(self.actorIndex.__len__()):
            name = ""
            for s in self.actorIndex[i].keys():
                name += s
            num = 0.0
            for i in self.emotionDict[i].values():
                num += i
            print name + " : " + str(num)
	
	# 把结果写入文件
        with open(resultPath, 'w') as file:
            data = ''
            for i in range(self.actorIndex.__len__()):
                name = ""
                for s in self.actorIndex[i].keys():
                    name += s
                score = 0.0
                for n in self.emotionDict[i].values():
                    score += n
                data += name + ": " + str(score) + "\n"
            file.write(data)
        print "write file is ok"


    def printList(self, List):
        for aDict in List:
            name = ""
            index = 0
            value = 0.0
            for string in aDict.keys():
                if isinstance(string, str):
                    name += string
                else:
                    index += string
            for val in aDict.values():
                value += val
            if name.__len__() != 0:
                print "{" + name + ":" + str(value) + "}, ",
            else:
                print "{" + str(index) + ":" + str(value) + "}, ",


    def read_file(self, path):
        # 读取相关人名文件，完善三条类字典
        with open(path) as file:
            lines = file.readlines()
            index = 0
            for line in lines:
                self.emotionDict.append({index: 0})  # 【{0:0}，{1:0}，{2:0}……】
                words = line.strip().split("-")
                self.actorIndex.append({words[0]: index})  # 【{演员1:0}，{演员2:0}，……】
                for w in words:
                    self.nameIndex.append({w: index})  # 【{名称1:0}，{名称2:0}……{名称8:1}，{名称9:1}……】
                    self.nameScore.append({w: 0})  # 【{名称1:0}，{名称2:0}……{名称8:0}，{名称9:0}……】
                index += 1

    def emotionCalculate(self, name, sent, comment_score):
        # 情感分析，（已经确定name在句子str里面了）累加该评论中含有该名字的短句感情值
        emotion = 0.0
        num = 0
        s_em = 0
        for s in re.split(',|\.|;|，|。|；|！', sent):
            if s.find(name) != -1:
                em = sentiment.classify(sent.decode('utf8')) - 0.5
                s_em += em
                num += 1
                print s
                print "em: " + str(em)
        if num >= 1:
            emotion += s_em * 1.0 / num
        result = 0.5 * int(comment_score) + 5 * emotion
        print "0.5 * " + comment_score + ", 5 *" + str(emotion) + " => " + str(result)
        return result

    def testEmotionModel(self, path):
	# 测试训练出来的情感模型，如果模型打分0.6一下，并且该评论分低于3分，认为正确
        num = 0.0
        with open(path) as file:
            lines = file.readlines()
            for line in lines:
                arr = line.split("\001")
                com = arr[5].replace("\"", "").replace("\n", "")
                score = int(arr[4]) / 10
                em = sentiment.classify(com)
                print com
                print "score is " + str(score) + " emotion is " + str(em)
                if em < 0.6 and score < 3:
                    num += 1.0
                else:
                    if em >= 0.6 and score >= 3:
                        num += 1.0

        return num


    def actor_score(self, comPath):
        with open(comPath) as aFile:  # file是所有的评论
            lines = aFile.readlines()
            for line in lines:  # line是一条评论
                arr = line.split("\001")
                if arr.__len__() != 6:  # 如果切分后的评论没有6段长，则弃掉
                    line = ""
                else:
                    line = arr[5].replace("\"", "").replace("\n", "")  # 取评论
                    comment_score = re.sub("\D", "", arr[4])  # 取整条评论的分数
                string = re.findall("#(.*?)#", line)  # 去除评论有两个井号的地方
                if string.__len__() != 0:
                    find = ""
                    for s in string:
                        find += s
                    line = line.replace("#" + find + "#", "")
                # print line
                exe_name = [""]
                for index in range(self.nameIndex.__len__()):
                    name = ""
                    num = 0
                    for n in self.nameIndex[index].keys():
                        name += n
                    for n in self.nameIndex[index].values():
                        num += n
                    if line.__contains__(name):
                        has_exist = False
                        for n in exe_name:
                            if n.__contains__(name):
                                has_exist = True
                                break
                        if not has_exist:
                            # print name + " is in"
                            exe_name.append(name)
                            self.nameScore[index][name] += self.emotionCalculate(name, line, comment_score)
                            # print ""
            # print "---------------------------------------"
        for i in range(self.nameIndex.__len__() - 1):
            name = ""
            for s in self.nameIndex[i].keys():
                name += s
            index = 0
            for s in self.nameIndex[i].values():
                index += s
            self.emotionDict[index][index] += self.nameScore[i][name]

    def create_neg_pos_file(self):
        #  完成neg/pos
        neg = []
        pos = []
        with open("/media/yingying/新加卷/Yinglish/result.txt") as file:
            lines = file.readlines()

            for line in lines:
                split_line = line.split("\001")
                if split_line.__len__() == 6:
                    score = re.sub("\D", "", split_line[4])
                    comment = split_line[5].strip().replace("\n", "")
                    if int(score) >= 9:
                        pos.append(comment.replace("\"", "") + "\n")
                    else:
                        if int(score) <= 3:
                            neg.append(comment.replace("\"", "") + "\n")
        with open("emotion_file/pos", 'w') as posFile:
            for ramStr in random.sample(pos, 7000):
                posFile.write(ramStr)
        print "pos file has been created"

        with open("emotion_file/neg", 'w') as negFile:
            for ramStr in random.sample(neg, 7000):
                negFile.write(ramStr)
        print "neg file is ok!"


if __name__ == "__main__":
    comDir = "/media/yingying/新加卷/Yinglish/comment总/WeiBoMovie/"
    nameDir = "/media/yingying/新加卷/Yinglish/20102017人名/WeiBoName/"
    emotionDir = "/media/yingying/新加卷/Yinglish/emotionScore/WeiBoEmotion/"
    pathDir = os.listdir(comDir)
    for allDir in pathDir:
        result_file = emotionDir + allDir
        if os.path.exists(result_file):
            continue
        if not os.path.exists(nameDir + allDir):
            continue
        ae = actor_emotion_score(nameDir + allDir, comDir + allDir, emotionDir + allDir)
        del ae
     namePath, comPath, resultPath
    namePath =  "/media/yingying/新加卷/Yinglish/20102017人名/WeiBoName/记忆大师"
    comPath = "/media/yingying/新加卷/Yinglish/comment总/test2"
    resultPath = "/media/yingying/新加卷/Yinglish/20102017人名/WeiBoName/记忆大师EmotionResult"
    ae = actor_emotion_score(namePath, comPath, resultPath)

    # ae = actor_emotion_score()
    # print ae.testEmotionModel("/media/yingying/新加卷/Yinglish/comment总/testModel.txt")
