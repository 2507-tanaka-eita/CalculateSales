package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
	private static final String FILE_INVALID_FILE_NUM = "売上ファイル名が連番になっていません";
	private static final String CONTENTS_SALESPRICE_OVER = "合計⾦額が10桁を超えました";
	private static final String CONTENTS_BRANCHCODE = "の支店コードが不正です";
	private static final String CONTENTS_INVALID_FORMAT = "のフォーマットが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// コマンドライン引数が渡されているかチェック (エラー処理3) -----
		if (args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();

		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		// 売上ファイルのみを保持するList(ファイルパス、ファイル名)
		List<File> rcdFiles = new ArrayList<>();

		// 支店定義ファイル読み込み処理
		if (!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}

		// 売上ファイルの集計処理 (処理内容2-1、2-2) -----

		// ***処理内容2-1
		// コマンドライン引数で指定したディレクトリ内のファイル/フォルダをすべて取得
		File[] files = new File(args[0]).listFiles();

		// files変数に格納されたデータから順番にファイル名を取得
		for (int i = 0; i < files.length; i++) {
			String filesName = files[i].getName();

			// 売上ファイルがファイル情報なのか確認 (エラー処理3) -----
			// 確認結果：trueだったらList(rcdFiles)へ取得結果を追加
			if (files[i].isFile() && filesName.matches("^[0-9]{8}\\.rcd$")) {
				rcdFiles.add(files[i]);
			}
		}

		// 売上ファイルのリストを昇順で並び替え
		Collections.sort(rcdFiles);

		// 売上ファイル名が連番になっているかチェック (エラー処理2-1) -----
		for (int i = 0; i < rcdFiles.size() - 1; i++) {
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));

			if ((latter - former) != 1) {
				System.out.println(FILE_INVALID_FILE_NUM);
				return;
			}
		}

		// ***処理内容2-2
		BufferedReader br = null;

		// rcdFiles変数から順番にデータを読み込む
		for (int i = 0; i < rcdFiles.size(); i++) {
			try {
				// 売上ファイルの中身を読み込む
				File file = rcdFiles.get(i);
				FileReader fr = new FileReader(file);
				br = new BufferedReader(fr);

				// 売上ファイルの中身を保持するList
				List<String> rcdContents = new ArrayList<>();

				// List(rcdContents)に売上ファイルの中身を格納
				String line;
				while ((line = br.readLine()) != null) {
					rcdContents.add(line);
				}

				// 売上ファイルの中身が2行で書かれているかチェック (エラー処理2-4) -----
				if (rcdContents.size() != 2) {
					System.out.println(rcdFiles.get(i).getName() + CONTENTS_INVALID_FORMAT);
					return;
				}

				// 売上ファイルの支店コードが支店定義ファイル内に存在するかチェック (エラー処理2-3) -----
				if (!branchNames.containsKey(rcdContents.get(0))) {
					System.out.println(rcdFiles.get(i).getName() + CONTENTS_BRANCHCODE);
					return;
				}

				// 支店コード、売上額を変数で定義
				String branchCode = rcdContents.get(0);
				String salesPrice = rcdContents.get(1);

				// 売上額が数字なのかをチェック (エラー処理3) -----
				if (!salesPrice.matches("^[0-9]+$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				// 売上額をMapに加算していくための型変換(String→Long)
				long fileSale = Long.parseLong(salesPrice);

				// 読み込んだ売上金額をMap(branchSales)に加算していく処理
				Long saleAmount = branchSales.get(branchCode) + fileSale;

				// 売上額の合計が10桁以上になっていないかチェック(エラー処理2-2) -----
				if (saleAmount >= 10000000000L) {
					System.out.println(CONTENTS_SALESPRICE_OVER);
					return;
				}

				// 加算した売上⾦額をMapに追加
				branchSales.put(branchCode, saleAmount);

			} catch (IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;

			} finally {
				// ファイルを開いている場合
				if (br != null) {
					try {
						// ファイルを閉じる
						br.close();

					} catch (IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}
		}

		// 支店別集計ファイル書き込み処理
		if (!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
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
			// ファイル有無を確認 (エラー処理1) -----
			if (!file.exists()) {
				System.out.println(FILE_NOT_EXIST);
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			// 支店定義ファイルの読み込み処理 (処理内容1-2) -----

			// 支店別定義ファイルの中身を一行ずつ読み込む
			String line;
			while ((line = br.readLine()) != null) {
				// ***処理内容1-2
				// カンマをキーに支店コードと支店名を分割し配列に格納
				String[] items = line.split(",");

				// 読み取った情報が指定のフォーマットになっているかチェック (エラー処理1) -----
				if ((items.length != 2) || (!items[0].matches("^[0-9]{3}$"))) {
					System.out.println(FILE_INVALID_FORMAT);
					return false;

				}

				// readLineで読み取った情報をMapに追加
				branchNames.put(items[0], items[1]);
				branchSales.put(items[0], 0L);
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;

		} finally {
			// ファイルを開いている場合
			if (br != null) {
				try {
					// ファイルを閉じる
					br.close();

				} catch (IOException e) {
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
			for (String key : branchNames.keySet()) {
				// 支店コード、支店名、売上額を「branch.out」ファイルに書き込み
				bw.write(key + "," + branchNames.get(key) + "," + branchSales.get(key));
				bw.newLine();
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;

		} finally {
			// ファイルを開いている場合
			if (bw != null) {
				try {
					// ファイルを閉じる
					bw.close();

				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}
}
