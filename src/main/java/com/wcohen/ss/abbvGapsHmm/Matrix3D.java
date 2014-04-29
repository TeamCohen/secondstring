package com.wcohen.ss.abbvGapsHmm;

/**
 * M-by-N-by-K matrix implementation.
 */
final public class Matrix3D {
    private final int M;             // dim1
    private final int N;             // dim2
    private final int K;             // dim3
    private final double[][][] data;   // M-by-N-by-K array

    // create M-by-N matrix of 0's
    public Matrix3D(int M, int N, int K) {
        this.M = M;
        this.N = N;
        this.K = K;
        data = new double[M][N][K];
    }

    // create matrix based on 3d array
    public Matrix3D(double[][][] data) {
        M = data.length;
        N = data[0].length;
        K = data[0][0].length;
        this.data = new double[M][N][K];
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
            	for (int l = 0; l < K; l++)
                    this.data[i][j][l] = data[i][j][l];
    }

    // copy constructor
    private Matrix3D(Matrix3D A) { this(A.data); }

    // create and return a random M-by-N matrix with values between 0 and 1
    public static Matrix3D random(int M, int N, int K) {
        Matrix3D A = new Matrix3D(M, N, K);
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
            	for (int l = 0; l < K; l++)
            		A.data[i][j][l] = Math.random();
        return A;
    }

    // create and return the N-by-N identity matrix
    public static Matrix3D identity(int N) {
        Matrix3D I = new Matrix3D(N, N, N);
        for (int i = 0; i < N; i++)
            I.data[i][i][i] = 1;
        return I;
    }

    // return C = A + B
    public Matrix3D plus(Matrix3D B) {
        Matrix3D A = this;
        if (B.M != A.M || B.N != A.N || B.K != A.K) throw new RuntimeException("Illegal matrix dimensions.");
        Matrix3D C = new Matrix3D(M, N, K);
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
            	for (int l = 0; l < K; l++)
            		C.data[i][j][l] = A.data[i][j][l] + B.data[i][j][l];
        return C;
    }


    // return C = A - B
    public Matrix3D minus(Matrix3D B) {
        Matrix3D A = this;
        if (B.M != A.M || B.N != A.N || B.K != A.K) throw new RuntimeException("Illegal matrix dimensions.");
        Matrix3D C = new Matrix3D(M, N, K);
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
            	for (int l = 0; l < K; l++)
            		C.data[i][j][l] = A.data[i][j][l] - B.data[i][j][l];
        return C;
    }

    // does A = B exactly?
    public boolean eq(Matrix3D B) {
        Matrix3D A = this;
        if (B.M != A.M || B.N != A.N || B.K != A.K) throw new RuntimeException("Illegal matrix dimensions.");
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
            	for (int l = 0; l < K; l++)
            		if (A.data[i][j][l] != B.data[i][j][l]) return false;
        return true;
    }

    // return C = A * s
    public Matrix3D times(double s) {
        Matrix3D A = this;
        Matrix3D C = new Matrix3D(A);
        for (int i = 0; i < C.M; i++)
            for (int j = 0; j < C.N; j++)
                for (int l = 0; l < C.K; l++)
                    C.data[i][j][l] *= s;
        return C;
    }

    // return an entry in the matrix
    public double at(int i, int j, int l){
    	return data[i][j][l];
    }
    
    public void set(int i, int j, int l, double val){
    	data[i][j][l] = val;
    }
    
    public void add(int i, int j, int l, double val){
    	data[i][j][l] += val;
    }
    
    public void mul(int i, int j, int l, double val){
    	data[i][j][l] *= val;
    }

    public int dimension1(){
    	return M;
    }
    
    public int dimension2(){
    	return N;
    }
    
    public int dimension3(){
    	return K;
    }

}

