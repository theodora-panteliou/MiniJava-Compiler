class Test{
    // public static void main(String[] a){
    //     int[] ar;
    //     ar = new int[4];
    //     ar[2] = 4;
    //     System.out.println(ar[2]);
    // }
    public static void main(String[] a){
        System.out.println(new BBS().Init());
    }

}

class BBS{
    
    int[] number ;
    int num;

    // Initialize array of integers
    public int Init(){
        number = new int[10] ;
        num = 0;
        
        number[4] = 7  ; 
        // number[2] = 12 ;
        // number[3] = 18 ;
        // number[4] = 2  ; 
        // number[5] = 11 ;
        // number[6] = 6  ; 
        // number[7] = 9  ; 
        // number[8] = 19 ; 
        // number[9] = 5  ;
        return 1;	
    }

}