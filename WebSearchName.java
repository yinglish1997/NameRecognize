package hanlp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import scala.collection.generic.BitOperations.Int;

import breeze.optimize.linear.LinearProgram.Integer;


public class WebSearchName {

	/**爬取对应问题的网页源码，匹配名字，返回频数最大的前五个名字
	 * @param args
	 */
	String findName;//要去匹配查找的未知名称
	String content ; //网页<div id = "content_left>到<div id = rs>部分的源码
	ArrayList<String> people; //演员角色数组，与NER中的必须相同
	
	LinkedHashMap<String, Double> nameFrequencyMap; // 键值对【名字：频数】
	String mateName;
	
	public WebSearchName(ArrayList<String> peopleArr, String url, String name) throws InterruptedException{
//		System.out.println("---------------------------------------------------------------------------");
//		System.out.println(name);
//		System.out.println(url);
		//peopleArr是本部电影所有的演员角色名字数组，URL是问题字符串， name是未知的名称
		this.findName = name;
		
		this.content = RegexString(catchSourceCode(url) 
				,"<div id=\"content_left\">(.+?)<div id=\"rs\">");		

		this.people = peopleArr;
		this.mateName = mateName();  //去网页查找匹配unKnow
		Thread.sleep(1000);
//		System.out.println(this.mateName);
	}

	public String mateName(){
	
		//计算各个演员角色名字出现的频数，取最高频的三个与未知名称计算相似度。
		//返回最高相似度，若无，返回最大频的名字
		HashMap<String, Double> set = new HashMap<String, Double>();//【名字，源码中出现的次数】
		
		for(String n: this.people )
			set.put(n, shortDistanceFrequency(this.content, n) );
		if(Collections.max(set.values()) == 0.0){
			return "null";
		}else{
		 LinkedHashMap<String, Double> sortedLink = sortByValue(set);
		 Iterator<String> sortIter = sortedLink.keySet().iterator();
		 ArrayList<String> sortList = new ArrayList<String>();
		 while(sortIter.hasNext())
			 sortList.add(sortIter.next());

		 return sortList.get(sortList.size() - 1);
		}
	};
	
	private double shortDistanceFrequency(String content, String n) {
		// 在源码中匹配 未知名称findName 前后各十个词语的句子，返回 名字n 在这些句子中出现的频数
		Pattern p = Pattern.compile(".{12}"+ this.findName + ".{12}");
		Matcher m = p.matcher(content);
	  //  ArrayList<String> matcherSentence = new ArrayList<String>();
		double num = 0.0;
	    while(m.find()){
	    	//matcherSentence.add(m.group());
	    	String tmp = m.group();
	    	//System.out.println(tmp);
	    	//System.out.println("\n");
	       Pattern p2 = Pattern.compile(n);
	       Matcher m2 = p2.matcher(tmp);  
	       while(m2.find())
	    	   num += 1;
	    }    
	    
		return num;
	}

	public  double jaccard(String f, String t){
		//计算两个词的杰西卡距离
		String s = f.replaceAll("\"", "").replaceAll(", ", "").replaceAll(" ", "");
		String ss = t.replaceAll("\"", "").replaceAll(", ", "").replaceAll(" ", "");
		char[] sChar = s.toCharArray();
		char[] ssChar = ss.toCharArray();
		
		Set<String> one = new HashSet<String>();
		Set<String> two = new HashSet<String>();
		Set<String> three = new HashSet<String>();
		for(char sc: sChar){
			one.add("" + sc);
			three.add("" + sc);
		}
		for(char ssc: ssChar)
			two.add("" + ssc);
		one.addAll(two);
		three.retainAll(two);
		//System.out.println(s + "," + ss + "," + "one's length: " + one.size());
		//System.out.println("three's length: " + three.size());ArrayList<Set<String>> nrAndJacNz
		return 1.0 * three.size() / one.size();
	}
    public LinkedHashMap<String, Double> sortByValue (Map<String, Double> map){
    	//
    	List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(map.entrySet());
    	Collections.sort(list, new Comparator<Map.Entry<String, Double>>(){
    		public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2){
    			return (o1.getValue()).compareTo(o2.getValue());
    		}
    	});
    	LinkedHashMap<String, Double> result = new LinkedHashMap<String, Double>();
    	for(Map.Entry<String, Double> entry: list)
    		result.put(entry.getKey(), entry.getValue());
    	
    	return result;
    }
    public LinkedHashMap<String, Double> sortFrequency(HashMap<String, Double> set){

		LinkedHashMap<String, Double> sortedMap = sortByValue(set);
//		for(String name: sortedMap.keySet())
//			System.out.println(name + ": " + sortedMap.get(name));
		return sortedMap;
    }
	public int countWordsFrequency(String text, String find){
		int count = 0;
		int index = 0;
		while( (index = text.indexOf(find, index)) != -1){
			index = index + find.length();
			count ++;
		}
		return count;
	}
	public String catchSourceCode(String url){
		String content = "";
		BufferedReader in = null;
		try{		
			URL realUrl = new URL(url);
			HttpURLConnection urlConnection =(HttpURLConnection) realUrl.openConnection();
			urlConnection.connect();
			in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "utf-8"));			
			String line;
			while((line = in.readLine()) != null)
				content += line;
			in.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		finally{
			try{
				if(in != null)
					in.close();
			}catch(Exception e2){
				e2.printStackTrace();
			}
		}
		return content;
		}
    public static String RegexString(String targetStr, String patternStr){
        //定义一个样式模板，此中使用正则表达式，括号中是要抓的内容
    	Pattern p = Pattern.compile("\\s*|\t|\r|\n");
    	Matcher m = p.matcher(targetStr);
    	String content = m.replaceAll("");
//    	System.out.println("contentis ");
//    	System.out.println(content.length());
    	
        p = Pattern.compile(patternStr);
        //定义一个matcher用来做匹配
        m = p.matcher(targetStr);
        if(m.find()){
            return m.group(1).trim().replaceAll("\\s*", "");
        }else 
        return "Nothing";
    }
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		/*
		 ,"冷锋","老爹""何建国","卓亦凡","瑞秋",,"舰长","龙小云","钱必达","干儿子土豆","Pasha""林志雄",
		 
	    String[] people = new String[]{"吴京","弗兰克·格里罗","吴刚","张翰","卢靖姗","石青松",
	    		"丁海峰","余男","于谦","Nwachukwu Kennedy ..","Diana Sylla"
	    		,"石兆琪","淳于珊珊","倪大红","Min Deng","肖竹","Wu Ji","Qiang Ma","Li Zhijun","Zibin Fang"
	    		,"Hostage","Sen Wang","38Shi","Tengyuan Liu","Yu Fei","Yongda Zhang","Shao Bing","Xiaolong Zhang"
	    		,"Ban Zhaun","赵毅","Chief of Staff","Zi Liang","Eagle","Guangping Guo","Father of Feng","茹萍","Mother of Feng"
	    		,"Doudou","Young Feng","Zhang Heng","Male Officer","Miaomiao Tian","Female Officer","Yuanyuan Shan"
	    		,"Female Officer","Mao San","Little Soldier","斯科特·阿特金斯","Tomcat (as Scott Edward Adkins)","凯文·李"
	    		,"Mad Cow","Christopher Collins","Cowboy (as Christopher Conrad Collins)","索纳·伊姆贝","Driver (as Joseph Eninganeyambe)"
	    		,"凯尔·夏皮罗","Monkey (as Kyle Lawrence Shaprio)","塞缪尔·西维尔奇","Assassin","Alberto Bossum","ICP A","Afshin","ICP B"};
		 */
	}
}

/*	
//	for(String n: this.people)
//		set.put(n, (countWordsFrequency(this.content, n) * 1.0));
    
	this.nameFrequencyMap = sortFrequency(set);// 频数从小到大排序呢的【名字，频数】

	
	Iterator<String> nf = this.nameFrequencyMap.keySet().iterator(); 
	ArrayList<String> list = new ArrayList<String>();//放置频数从小到大的有序名字
	//System.out.println("this.nameFrequencyMap 's key");
	while(nf.hasNext()){
		String n = nf.next();
		list.add(n);
		//System.out.print(n + "  ");
	}

	HashMap<String, Double> nameJaccard = new HashMap<String, Double>();//【名字，与输入名称的相似度】
	System.out.println("the threeFrequency: ");
	for(int i = list.size() - 1; i >= list.size() - 3; i --){
		//取频数最大的前三个来计算距离
		System.out.println(list.get(i) + "  " + this.nameFrequencyMap.get(list.get(i)));
		nameJaccard.put(list.get(i), jaccard(list.get(i), this.findName));
	}
	
	//从小到大排序的【名字，相似度】
	LinkedHashMap<String, Double> sortedJaccard = sortFrequency(nameJaccard);
	System.out.println("the similary: ");
	for(String s: sortedJaccard.keySet()){
		//取频数最大的前三个来计算距离
		System.out.println(s + "  " + sortedJaccard.get(s));
	}
	
	Iterator<String> sortedJaccardName = sortedJaccard.keySet().iterator();
	ArrayList<String> biggestJaccard = new ArrayList<String>();//相似度从小到大的名字
	while(sortedJaccardName.hasNext())
		biggestJaccard.add(sortedJaccardName.next());
	
	//如果存在相似度的差别，取相似度最大的。如果没有差别，取频数最大的为结果
	int lastIndex = biggestJaccard.size() - 1;
	if(sortedJaccard.get(biggestJaccard.get(lastIndex)) > sortedJaccard.get(biggestJaccard.get(lastIndex - 1) )){
		System.out.println("biggestJaccard: " + biggestJaccard.get(lastIndex));
		return biggestJaccard.get(lastIndex);
	}else{
		System.out.println("mostFrequency: " + list.get(list.size() - 1));
		
		return list.get(list.size() - 1);
	}*/
