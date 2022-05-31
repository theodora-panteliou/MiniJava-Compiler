%1 = alloca i32
%2 = alloca %struct.array*
store i32 0, i32* %1
%3 = call noalias nonnull i8* @_Znwm(i64 16) #2
%4 = bitcast i8* %3 to %struct.array*
store %struct.array* %4, %struct.array** %2
%5 = load %struct.array*, %struct.array** %2
%6 = getelementptr inbounds %struct.array, %struct.array* %5, i32 0, i32 0
store i32 2, i32* %6
%7 = call noalias nonnull i8* @_Znam(i64 8) #2
%8 = bitcast i8* %7 to i32*
%9 = load %struct.array*, %struct.array** %2
%10 = getelementptr inbounds %struct.array, %struct.array* %9, i32 0, i32 1
store i32* %8, i32** %10
ret i32 0

%1 = alloca i32
%2 = alloca %struct.array
store i32 0, i32* %1
%3 = getelementptr inbounds %struct.array, %struct.array* %2, i32 0, i32 0
store i32 2, i32* %3
%4 = call noalias nonnull i8* @_Znam(i64 8) #2
%5 = bitcast i8* %4 to i32*
%6 = getelementptr inbounds %struct.array, %struct.array* %2, i32 0, i32 1
store i32* %5, i32** %6
ret i32 0