import java.io.IOException;

public class mainRunMrosFPM {
    public static void main(String [] arg) throws IOException {
        //String inputFilePath = "TestFile/TestFile01";
        //String outputFilePath = ".//outputFile01.txt";
        //String inputFilePath = "addDataset/FIFA/addSign_0.9.txt";
        //String outputFilePath = ".//outputFIFA0.2.txt";
        String inputFilePath = "indeDataset/Sign/Sign_original.txt";
        String outputFilePath = "outputTest.txt";
        double minSupRe =0.3;
        double delta = 0.1;//deDB与原来的CDB的比例
        algoFpmMros algoFpmMros = new algoFpmMros();
        algoFpmMros.ReadFileToVerDB_Mros(inputFilePath,outputFilePath,minSupRe,1-delta);

//        System.out.println("******************start to increase pattern mining********************");
//        String inFilePath ="addDataset/Bible/Bible_0.1_2.txt";
//        String inOutPath = "addDataset/Bible/outadd020Bible.txt";
//        algoFpmMros.addMFP(inFilePath,inOutPath,minSupRe,1-delta);
//        System.out.println("******************end increase mining********************");
//
//        System.out.println("******************start to decrease pattern mining********************");
//        String deFilePath ="fullydataset/FIFA_0.1.txt";
//        String deOutPath ="fullydataset/Bible/ADe210FIFA.txt";
//        algoFpmMros.deMFP(deFilePath,deOutPath,minSupRe,delta);
//        System.out.println("******************end decrease mining********************");

        System.out.println("******************start to fully dynamic pattern mining********************");
        String inFilePath ="indeDataset/Sign/Sign_11_2.txt";
        String deFilePath ="indeDataset/Sign/Sign_01_1.txt";
        String fullyOutPath = "indeDataset/testSign204.txt";
        algoFpmMros.FullyMFP(inFilePath,deFilePath,fullyOutPath,minSupRe,1-delta);
        System.out.println("******************end fully dynamic mining********************");

    }
}
