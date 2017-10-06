package hanlp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.classification.NaiveBayes;
import org.apache.spark.mllib.classification.NaiveBayesModel;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.tree.DecisionTree;
import org.apache.spark.mllib.tree.model.DecisionTreeModel;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dictionary.py.Pinyin;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.CRF.CRFSegment;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;

public class NER {

	/**
	 * @param args
	 */
	ArrayList<String> allList;//放置所有的评论
	
	ArrayList<Set<String>> termCrfNrSet; //放置每一条评论被CRF识别为nr的词语
	ArrayList<Set<String>> termCrfNzSet;//放置每一条评论被CRF识别为nz的词语
	ArrayList<Set<String>> termHmmSet;//放置每一条评论被Hmm识别为nr/nz的词语
	ArrayList<Set<String>> commentNames; //评论，及识别出来的名称
	ArrayList<Set<String>> correctName;//放置标注的每一条评论的人名
	String movieName;
	ArrayList<String> actor;//只有演员的名字列表
	ArrayList<String> hasRole;//演员角色都有
	
	HashMap<String, Set<String>>  relaxtedNames; //【名字，相关昵称】
	ArrayList<String> searchedName;
	
	public NER(String comFile, String nameFile, String relaxtedName) throws InterruptedException{
		//this.allList = new ArrayList<String>();
		this.allList = readComment(comFile);
		//this.allList.add("因为黄渤老师@黄渤和徐大才女@鸡毛蒜皮与鸡毛蒜皮入了坑......还是很值得[haha]\n喜欢每一个演员的表演...这是影片最大的亮点...少看过精分的黄渤老师，头次遇见心理扭曲的段奕宏警官和反面的杨子珊医生，感谢始终相信“老公”是好人的徐大才女...即使​");
		segSentence crfHr = new segSentence(true, "nr");
		this.termCrfNrSet = crfHr.tSet;
		segSentence crfHz = new segSentence(true, "nz");
		this.termCrfNzSet = crfHz.tSet;
		segSentence hmmHr = new segSentence(false, "nrz");
		this.termHmmSet = hmmHr.tSet;	

		this.correctName = readName("/media/yingying/新加卷/Yinglish/comment总/test2标注");//读取标注：每一条评论中的人名
		//System.out.println(this.allList.size() + " vs " + this.correctName.size());
		String[] arr = nameFile.split("/");
		this.movieName = arr[arr.length - 1];
		System.out.println(this.movieName);
		//读取标注的该部电影的演员及角色名。
		//同时完善名字链表name
		this.relaxtedNames = new HashMap<String, Set<String>>();
		this.actor = readNameAndRole(nameFile, false); 
		this.hasRole = readNameAndRole(nameFile, true); 
		this.searchedName = new ArrayList<String>();//用来记录已经搜索过的名字，避免重复搜索
	 
		//扩充NR队列
		ArrayList<Set<String>> crfnz = nzTOnr(this.termCrfNzSet);
		ArrayList<Set<String>> hmm = nzTOnr(this.termHmmSet);
	
	this.commentNames = new ArrayList<Set<String>>();//分评论存储识别出来的人名
    ArrayList<String> totalNr = new ArrayList<String>();//不分评论，统一放在同一链表
	for(int i = 0; i < crfnz.size(); i ++){
		Set<String> newSet = new HashSet<String>();
		for(String s: termCrfNrSet.get(i)){
			newSet.add(HanLP.convertToSimplifiedChinese(s));
			totalNr.add(HanLP.convertToSimplifiedChinese(s));
		}
		for(String s: crfnz.get(i)){
			newSet.add(HanLP.convertToSimplifiedChinese(s));
			totalNr.add(HanLP.convertToSimplifiedChinese(s));
		}
		for(String ss: hmm.get(i)){
			newSet.add(HanLP.convertToSimplifiedChinese(ss));
			totalNr.add(HanLP.convertToSimplifiedChinese(ss));
		}
		this.commentNames.add(newSet);
	}
		
	//calculate(totalNr);
	//writeNames(relaxtedName);
	ArrayList<Set<String>> res = calculateComNames(this.commentNames);
	
	//计算准确率及召回率
	System.out.println("CRFnr: ");
	accuracyAndrecall(this.termCrfNrSet);
	System.out.print("\n");
	System.out.println("CRFnr + nz + webSearch: ");
	accuracyAndrecall(res);
	//testRecall(this.nameSet);
	//printLists(termCrfNrSet, termCrfNzSet, termHmmSet, crfnz ,hmm, this.commentNames, res);
	System.out.print("\n");
	for(int i = 0; i < this.allList.size(); i ++){
		System.out.println(this.allList.get(i));
		System.out.println("crfNr: ");
		for(String s: this.termCrfNrSet.get(i))
			System.out.print(s + "  ");
		System.out.print("\n");
		System.out.println("finalRecognizeNames: ");
		for(String s: res.get(i))
			System.out.print(s + "   ");
		System.out.print("\n");
	}
	
	for(int i = 0; i < this.allList.size(); i ++){
		System.out.println(this.allList.get(i));
		for(String s: res.get(i)){
			System.out.print(s + " ");
		}
		System.out.print("\n");
	}
	
	for(String name: this.relaxtedNames.keySet()){			
		String[] array = name.split("-");
		ArrayList<String> list = new ArrayList<String>();
		for(String s: array){
			list.add(s);
			System.out.print(s + " ");
		}					
		for(String relaxted: this.relaxtedNames.get(name)){					
			if( !list.contains(relaxted) ){
				list.add(relaxted);
				System.out.print(relaxted + " ");
			}
		}
		System.out.print("\n");
	}/**/
	}	
	public boolean samePinYin(String str, String string){
		if(str.length() == 2 && string.length() == 3){
			string = string.substring(1);
		}
		List<Pinyin> one = HanLP.convertToPinyinList(str);
		List<Pinyin> two = HanLP.convertToPinyinList(string);
		String oneStr = ""; String twoStr = "";
		for(Pinyin py: one)
			oneStr += py.getPinyinWithoutTone();
		for(Pinyin py: two)
			twoStr += py.getPinyinWithoutTone();
		if(oneStr.equals(twoStr))
			return true;
		else return false;
	}
    public ArrayList<Set<String>> calculateComNames(ArrayList<Set<String>>  commentNames) throws InterruptedException {
    	ArrayList<Set<String>> result = new ArrayList<Set<String>>();
  	   for(Set<String> set: commentNames){
  		   Set<String> newSet = new HashSet<String>();
  		   for(String aName: set){
  			    boolean jacMate = false;
  			   System.out.println(aName);
  			   for(String aset: this.relaxtedNames.keySet()){
  				   String[] arr = aset.split("-");
  				   for(String n: arr){			  
	   				   if(samePinYin(aName, n) | jaccard(aName, n) >= 0.6){
	   					    System.out.println(aName + " <- jaccard -> " + n);
	  					   newSet.add(aName);
	  					   this.relaxtedNames.get(aset).add(aName);
	  					   jacMate = true;
	  					   break;
	  				   } 					   
  				   }
  			   }
  			   if(! jacMate){
  				   System.out.println(aName + " need to search");
  				   if(!this.searchedName.contains(aName) && (aName.length() > 1) && (aName.length() < 4)){
  					   System.out.println("searching: " + aName);
  					   this.searchedName.add(aName);
  					   String url = "http://www.baidu.com/s?ie=utf-8&f=8&rsv_bp=1&tn=baidu&wd=" + this.movieName + "中的" + aName + "是谁";
  					   WebSearchName web = new WebSearchName(this.actor, url, aName);
  					   String mate = web.mateName;
  					   System.out.println(mate);
  					   for(String s: this.relaxtedNames.keySet()){
  						   String[] ar = s.split("-");
  						   for(String a: ar){
  							   if(mate.equals(a)){
  								   newSet.add(aName);
  								   this.relaxtedNames.get(s).add(aName);
  								   break;
  							   }
  						   }					  
  					   }
  				   }
  			   }
  			  System.out.println("------------------------------------------------------------------");
  		   }
  		   result.add(newSet);
  	   }
  	   return result;
     } 
	public void writeNames(String path){
		File file = new File(path);
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
			for(String name: this.relaxtedNames.keySet()){			
				String[] arr = name.split("-");
				String str = "";
				ArrayList<String> list = new ArrayList<String>();
				for(String s: arr){
					str += s + " ";
					list.add(s);
				}					
				bw.write(str);
				for(String relaxted: this.relaxtedNames.get(name)){					
					if( !list.contains(relaxted) )
							bw.write(relaxted + " ");
				}
				bw.write("\n");
			}
			bw.close();
			System.out.println("file write is ok");
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	  public static int minEditDistance(String dest, String src) { 
	        int[][] f = new int[dest.length()+1][src.length() + 1]; 
	        f[0][0] = 0; 
	        for (int i = 1; i < dest.length() + 1; i  ++) { 
	            f[i][0] = i; 
	        } 
	        for (int i = 1; i < src.length() + 1; i  ++) { 
	            f[0][i] = i; 
	        } 
	        for (int i = 1; i < dest.length() + 1; i  ++) { 
	            for (int j = 1; j < src.length() + 1; j  ++) { 
	                // 替换的开销 
	                int cost = 0; 
	                if (dest.charAt(i - 1) != src.charAt(j - 1)) { 
	                    cost = 1; 
	                } 
	                int minCost; 
	                if (f[i - 1][j] < f[i][j - 1]) { 
	                    minCost = f[i - 1][j] + 1; 
	                } else { 
	                    minCost = f[i][j - 1] + 1; 
	                } 
	                if (minCost > f[i - 1][j - 1] + cost) { 
	                    minCost = f[i - 1][j - 1] + cost; 
	                } 
	                f[i][j] = minCost; 
	            } 
	        } 
	        return f[dest.length()][src.length()]; 
	  }
   public ArrayList<Set<String>> nzTOnr(ArrayList<Set<String>> nz){
	   //筛选nz为nr，计算nz与演员角色的相似度，超过0.1的保留
	   ArrayList<Set<String>> setArr = new ArrayList<Set<String>>();
	   for(Set<String> set: nz){
		   Set<String> filtedSet = new HashSet<String>();
		   for(String s: set){
			   	for(String n: this.hasRole){
			   		if(minEditDistance(s, n) == 0.0){//计算拼音的最小编辑距离，可能有打错字的情况
			   			filtedSet.add(s);
			   		}else{
			   			if(jaccard(s, n) >= 0.1)
			   			filtedSet.add(s);
			   		}			   		
			   	}
		   }
		   setArr.add(filtedSet);
	   }
	return setArr;
   }
   public void calculate(ArrayList<String>  totalNr) throws InterruptedException{
	   for(String unKnow: totalNr){
		   boolean jacMate = false;
		   for(String name: this.relaxtedNames.keySet()){
			   String[] arr = name.split("-");		   
			   for(String a: arr){
				   if( (jaccard(a, unKnow) >= 0.6) ){
						   this.relaxtedNames.get(name).add(unKnow);
						   jacMate = true;
				   	}
				}		   
			}
		   if(!jacMate){
			   if(!this.searchedName.contains(unKnow) && (unKnow.length() > 1) && (unKnow.length() < 4)){
				   this.searchedName.add(unKnow);			   
				    String url = "http://www.baidu.com/s?ie=utf-8&f=8&rsv_bp=1&tn=baidu&wd=" + this.movieName + "  " + unKnow;
				    WebSearchName web = new WebSearchName(this.actor, url, unKnow);
				    String mate = web.mateName;
				    for(String name: this.relaxtedNames.keySet()){
				    	String[] arr = name.split("-");
				    	for(String a: arr){
				    		if(a.equals(mate))
				    			this.relaxtedNames.get(name).add(unKnow);
				    	}
				    }
			   }
		   }
	   }
   }
	private ArrayList<String> readNameAndRole(String path, boolean hasRole) {
		//读取标注的该部电影的演员角色名字
		ArrayList<String> list = new ArrayList<String>();
		File file = new File(path);
		try{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String tmp = "";
			while( (tmp = br.readLine()) != null){
				if(tmp.contains("-")){					
					String[] arr = tmp.split("-");	
					list.add(arr[0]);					
					if(hasRole){
						for( int i = 1; i < arr.length; i ++){
						//list.add(arr[i]);
							list.add(arr[i]);
						}								
					}
				}
				this.relaxtedNames.put(tmp, new HashSet<String>());
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		return list;
	}

	public void testJaccard(ArrayList<Set<String>> crfnz, ArrayList<Set<String>> hmmnr, ArrayList<Set<String>> hmmnz){
		//打印输出各链表计算杰西卡距离的效果
		for(int i = 0; i < crfnz.size(); i ++){
			System.out.println(this.allList.get(i));
			
			System.out.println("crfHz");
			for(String s: this.termCrfNzSet.get(i))
				System.out.print(s + "  ");
			System.out.print("\n");
			
			System.out.println("JacccrfHz");
			for(String s: crfnz.get(i))
				System.out.print(s + "  ");
			System.out.print("\n");
			
			System.out.println("hmmnr");
			for(String s: this.termHmmSet.get(i))
				System.out.print(s + "  ");
			System.out.print("\n");
			
			System.out.println("JaccHmmnr");
			for(String s: hmmnr.get(i))
				System.out.print(s + "  ");
			System.out.print("\n");
			
			System.out.println("JaccHmmnz");
			for(String s: hmmnz.get(i))
				System.out.print(s + "  ");
			System.out.print("\n");
		}	
	}
/*
	public void testRecall(ArrayList<Set<String>> nrAndJacNz){
		//打印输出被漏掉的名字
		System.out.println("recall test: ");
		for(int i = 0; i < this.correctName.size(); i ++){
			for(String s: this.correctName.get(i)){
				if( ! nrAndJacNz.get(i).contains(s)){
					System.out.println(i + ": " + s);
				}
			}
		}	
	}*/
	ArrayList<String> loadDict(String fileName){
        //读取字典，并保存为Dictionary结构
    	ArrayList<String> Dict = new ArrayList<String>(); 
        File file = new File(fileName);
        try {
            //判断文件是否存在
            InputStreamReader read = new InputStreamReader(new FileInputStream(file));
            BufferedReader bufferedReader = new BufferedReader(read);
            String lineTxt = null;
            while((lineTxt = bufferedReader.readLine()) != null){
            	lineTxt = lineTxt.replaceAll("\n", "");
                Dict.add(lineTxt);
                } 
            bufferedReader.close();
            } 
        catch (Exception e) {  
                e.printStackTrace(); 
        }         
    	return Dict;
    }

	public void accuracyAndrecall( ArrayList<Set<String>> res ){
		double tt = 0.0; //  正确识别的名字  识别为名字t & 确实是名字t
		double recognizeAllName = 0.0; //所有识别的名字（单条评论去重）
		double allTheCorrectName = 0.0;//标注中一共有多少个名字（单条评论去重）
		//if(nrAndJacNz.size() == this.correctName.size()){}
			for(int i = 0; i < this.correctName.size(); i ++){
				allTheCorrectName += this.correctName.get(i).size();
				for(String name: res.get(i)){
					recognizeAllName += 1;
					if(this.correctName.get(i).contains(name))
						tt += 1;
				}
			}						
		System.out.println("一共有多少个名字（单条评论去重）" + allTheCorrectName);
		System.out.println("识别出来的名字（单条评论去重）" + recognizeAllName);
		System.out.println("正确识别的名字" + tt);
		System.out.println("Accuracy: "  + tt / recognizeAllName);
		System.out.println("Recall: "  + tt / allTheCorrectName);
	}
/**/
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
	
    public void printLists(ArrayList<Set<String>> crfnr, ArrayList<Set<String>> CrfNz, ArrayList<Set<String>> HMM, ArrayList<Set<String>> crfnz, ArrayList<Set<String>> hmm, ArrayList<Set<String>> comName,  ArrayList<Set<String>> setNames){
		//打印输出各个类变量链表，检查错误
		if(crfnr.size() == crfnz.size() &&  crfnz.size() == hmm.size()){
			for(int i = 0; i < this.allList.size(); i ++){
				System.out.println(i + " " + this.allList.get(i));
				
				System.out.print("crfNr: " );
				for(String s: crfnr.get(i))
					System.out.print(s + ",");
				System.out.print("\n");
				
				System.out.print("CrfNz: " );
				for(String s: CrfNz.get(i))
					System.out.print(s + ",");
				System.out.print("\n");
				
				System.out.print("HMM: " );
				for(String s: HMM.get(i))
					System.out.print(s + ",");
				System.out.print("\n");
				
				System.out.print("crfNz: " );
				for(String s: crfnz.get(i))
					System.out.print(s + ",");
				System.out.print("\n");
				
				System.out.print("hmm(nr+nz): " );
				for(String s:hmm.get(i))
					System.out.print(s + ",");
				System.out.print("\n");
				
				System.out.print("comNames: " );
				for(String s:comName.get(i))
					System.out.print(s + ",");
				System.out.print("\n");
			
				System.out.print("finalNames: " );
				for(String s:setNames.get(i))
					System.out.print(s + ",");
				System.out.print("\n");
			}
		}
    }

/*	*/
	public ArrayList<Set<String>> readName(String path){
		//读取对每一条评论标注了的人名
		File file = new File(path);
		ArrayList<Set<String>> list = new ArrayList<Set<String>>();
		try{
			BufferedReader br =new BufferedReader(new FileReader(file));
			String tmp = "";
			while( (tmp = br.readLine()) != null){
				if(!tmp.equals("empty")){
					String[] names = tmp.split(",");
					Set<String> aList =new HashSet<String>();
					for(String n: names){
							aList.add(n);
					}
					list.add(aList);					
				}
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		return list;
	}
	public  class segSentence{
		List<Term> term = new ArrayList<Term>();
		ArrayList<Set<String>> tSet = new ArrayList<Set<String>>();
		
		public segSentence(boolean crf, String pattern){
			//对allList中的句子进行切词识别，若crf=true表使用CRFSegment，否则用默认切词器hmm
			//若nr=true，表示取词性为nr的，否则取nz。
			//返回的是ArrayList<List<String>>，每一条链表对应一个评论，每个词是切出来的nr/nz。
			for(String comment: allList){
				Set<String> filtedList = new HashSet<String>(); 
				if(crf){
					Segment seg = new CRFSegment();
					this.term = seg.seg(comment);
				}else{
					this.term = HanLP.segment(comment);
				}
				for(Term t: this.term){
					if(pattern.equals("nr")){
						
						  if((t.nature.toString().contains("nr") )&& t.word.length() > 1){
							  filtedList.add(t.word.toString().replaceAll("\"", "").replaceAll(", ", "").replaceAll(" ", ""));
						  }
					}else if(pattern.equals("nz")){
						if((t.nature.toString().contains("nz") )&& t.word.length() > 1){
							  filtedList.add(t.word.toString().replaceAll("\"", "").replaceAll(", ", "").replaceAll(" ", ""));
						  }
					} else if((t.nature.toString().contains("nr") || (t.nature.toString().contains("nz")) )&& t.word.length() > 1){
						//System.out.println(t);
						  filtedList.add(t.word.toString().replaceAll("\"", "").replaceAll(", ", "").replaceAll(" ", ""));
					}					
				}
				this.tSet.add(filtedList);
			}			
		}
	}	

	public ArrayList<String> readComment(String path){
		File file = new File(path);
		ArrayList<String> list = new ArrayList<String>();
		try{
			BufferedReader br = new BufferedReader(new FileReader(file));		
			String tmp = "";
			int i = 0;
			while( (tmp  = br.readLine()) != null){
				if(i > 200){
					break;
				}
				String com = tmp.split("")[5];
				Pattern p = Pattern.compile("#(.*?)#");
				Matcher m = p.matcher(com);
				if(m.find())
					list.add(com.replace(m.group(1), "").replace("#", ""));
				else list.add(com);
				i ++;		
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		return list;
	}

	public static void main(String[] args) throws InterruptedException {
		// TODO Auto-generated method stub

	
		String comDir = "/media/yingying/新加卷/Yinglish/comment总/test2";
		String nameDir = "/media/yingying/新加卷/Yinglish/20102017人名/WeiBoName/记忆大师";
		String relaxtedDir = "/media/yingying/新加卷/Yinglish/comment总/test人名";
		NER reco = new NER(comDir, nameDir, relaxtedDir);
	
		/*	
		File dir = new File(comDir);
		File[] list = dir.listFiles();
		for(File aFile: list){
			String path = nameDir + aFile.getName().replace("\"", "").replace(".txt", "");
			File nameFile = new File(path);
			System.out.println(path);
			File result = new File(relaxtedDir + aFile.getName());
			if(nameFile.exists() && (!result.exists())){
				System.out.println(aFile.getName());
				NER reco = new NER(comDir + aFile.getName(), path, relaxtedDir + aFile.getName());
			}			
		}
		*/
	}
}
