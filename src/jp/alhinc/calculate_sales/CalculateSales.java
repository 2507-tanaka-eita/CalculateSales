package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();

		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		// 売上ファイルのみを保持するList(ファイルパス、ファイル名)
		List<File> rcdFiles = new ArrayList<>();

		// 売上ファイルの中身を保持するList
		List<String> rcdContents = new ArrayList<>();

		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}


		// 売上ファイルの集計処理 (処理内容2-1、2-2) -----

		// ***処理内容2-1
		// コマンドライン引数で指定したディレクトリ内のファイル/フォルダをすべて取得
		File[] files = new File(args[0]).listFiles();

		// files変数に格納されたデータから順番にファイル名を取得
		for(int i = 0; i < files.length; i++) {
			String filesName = files[i].getName();

			// 売上ファイルのみ取得できるよう正規表現で条件を追加
			// 条件：true の場合は、取得した情報をrcdFiles変数に格納
			if(filesName.matches("^[0-9]{8}\\.rcd$")) {
				rcdFiles.add(files[i]);
			}
		}

		// ***処理内容2-2
		BufferedReader br = null;

		try {
			// rcdFiles変数から順番にデータを読み込む
			for(int i = 0; i < rcdFiles.size(); i++) {

				// 売上ファイルの中身を読み込む
				File file = rcdFiles.get(i);
				FileReader fr = new FileReader(file);
				br = new BufferedReader(fr);

				// 支店コード、売上額をそれぞれ変数に格納
				String branchCode = br.readLine();
				String salesPrice = br.readLine();

				// List(rcdContents)に売上ファイルの中身を格納しておく *後の作業で使用
				rcdContents.add(branchCode);
				rcdContents.add(salesPrice);

				// 売上金額をMapに加算していくための型変換(String→Long)
				long fileSale = Long.parseLong(salesPrice);

				// 読み込んだ売上金額をMap(branchSales)に加算していく処理
				Long saleAmount = branchSales.get(branchCode) + fileSale;

				// 加算した売上⾦額をMapに追加
				branchSales.put(branchCode, saleAmount);
			}

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);

		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
				}
			}
		}

		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}
	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			// 支店定義ファイルの読み込み処理 (処理内容1-2) -----

			String line;
			// 支店別定義ファイルの中身を一行ずつ読み込む
			while((line = br.readLine()) != null) {

				// ***処理内容1-2
				// カンマをキーに支店コードと支店名を分割し配列に格納
			    String[] items = line.split(",");

			    // readLineで読み取った情報をMapに追加
			    branchNames.put(items[0], items[1]);
			    branchSales.put(items[0], 0L);
			}

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;

		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {

		// 支店別集計ファイルの出力処理(処理内容3-1) -----
		BufferedWriter bw = null;

		// ***処理内容3-1
		try {
			// コマンドライン引数で指定したディレクトリに支店別集計ファイル(branch.out)を作成
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			// Mapからすべてのkeyを取得
			for(String key : branchNames.keySet()) {

				// 支店コード、支店名、売上額を「branch.out」ファイルに書き込み
				bw.write(key + "," + branchNames.get(key) + "," + branchSales.get(key));
				bw.newLine();
			}

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;

		} finally {
			// ファイルを開いている場合
			if(bw != null) {
				try {
					// ファイルを閉じる
					bw.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

}
