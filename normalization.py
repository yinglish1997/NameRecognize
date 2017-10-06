#coding=utf-8

## 对结果进行归一化处理，每个演员的情感分除以该部电影所有演员的情感分总和
def normalize(name_dict):
    total_num = 0.0
    for name in name_dict:
        total_num += name_dict[name]
    for name in name_dict:
        name_dict[name] = name_dict[name] * 1.0 / total_num
    return name_dict


def read_file(filename):
    f = open(filename)
    lines = f.readlines()
    name_dict = {}
    for line in lines:
        line = line.strip()
        tokens = line.split(': ')
        print tokens
        name, score = tokens[0], float(tokens[1])
        name_dict[name] = score
    return name_dict

def save_file(filename, name_dict):
    line = ''
    for name in name_dict:
        line += name + ":" + str(name_dict[name]) + '\n'
    f = open(filename,'w')
    f.write(line)
    f.close()

import os
path_dir = '/media/yingying/新加卷/Yinglish/emotionScore/WeiBoEmotion'
norm_dir = '/media/yingying/新加卷/Yinglish/emotionScore/NormWeiBoEmotion'
for path, dir, filelist in os.walk(path_dir):
    for moviename in filelist:
        filename = path_dir + '/' + moviename
        print filename
        name_dict = read_file(filename)
        name_dict = normalize(name_dict)
        for name in name_dict:
            print name, name_dict[name]
        norm_filename = norm_dir + '/' + moviename
        save_file(norm_filename, name_dict)
        print 'ok'
