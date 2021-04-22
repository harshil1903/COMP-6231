//mpicc matrix.c -o matrix
//mpirun -np 4 matrix
//TO RUN the program, -np 4 means 4 number of processes allocated
//The program was executed in Ubuntu VM


#include <mpi.h>
#include <stdio.h>
#include <stdlib.h>

#define MATRIX_SIZE 5
#define A_ROWS MATRIX_SIZE            
#define B_COLUMNS MATRIX_SIZE              
#define FROM_MASTER 1         
#define FROM_SLAVE 2         

int main (int argc, char *argv[])
{
   int	size, rank, num_processes;                               
   int   source, dest, rows, average_rows, extra, offset, i, j, k, rc;                              
         
   double	A[A_ROWS][32], B[32][B_COLUMNS], Result[A_ROWS][B_COLUMNS];  

   MPI_Status status;

   MPI_Init(&argc,&argv);
   MPI_Comm_rank(MPI_COMM_WORLD,&rank);
   MPI_Comm_size(MPI_COMM_WORLD,&size);
   
   if (size < 2 ) 
   {
      printf("Can't run MPI with one task, Exiting \n");
      MPI_Abort(MPI_COMM_WORLD, rc);
      exit(1);
   }
   
   num_processes = size-1;


   //MASTER
   if (rank == 0)
   {
      printf("\nMPI Matrix Multiplication has started with %d tasks.\n", size);
      
      for (i = 0; i < A_ROWS; i++)
      {
         for (j = 0; j < 32; j++)
         {
            A[i][j]= i + j;
         }
      }
      for (i = 0; i < 32; i++)
      {
         for (j = 0; j < B_COLUMNS; j++)
         {
            B[i][j]= i + j;
         }
      }


      double start_time = MPI_Wtime();

      // Send matrix data to the slave tasks     
      average_rows = A_ROWS / num_processes; //Find average rows to send per process 
      extra = A_ROWS % num_processes;        //Find remaining rows after dividing
      offset = 0;                            //Initial offset set to 0 to send from ROW number 0
      
      
      for (dest = 1; dest <= num_processes; dest++)
      {
         rows = (dest <= extra) ? average_rows+1 : average_rows;   	
         
         printf("Sending %d rows to task %d; offset = %d\n", rows, dest, offset);
         
         MPI_Send( &offset, 1, MPI_INT, dest, FROM_MASTER, MPI_COMM_WORLD );
         
         MPI_Send( &rows, 1, MPI_INT, dest, FROM_MASTER, MPI_COMM_WORLD );
         
         MPI_Send( &A[offset][0], rows * 32, MPI_DOUBLE, dest, FROM_MASTER, MPI_COMM_WORLD );
         
         MPI_Send( &B, 32 * B_COLUMNS, MPI_DOUBLE, dest, FROM_MASTER, MPI_COMM_WORLD );
         
         offset = offset + rows;
      }

      // Receive results from slave tasks    
      for (i = 1; i <= num_processes; i++)
      {
         source = i;
      
         MPI_Recv( &offset, 1, MPI_INT, source, FROM_SLAVE, MPI_COMM_WORLD, &status );
      
         MPI_Recv( &rows, 1, MPI_INT, source, FROM_SLAVE, MPI_COMM_WORLD, &status );
      
         MPI_Recv( &Result[offset][0], rows * B_COLUMNS, MPI_DOUBLE, source, FROM_SLAVE, MPI_COMM_WORLD, &status );
         
         printf("Received results from task %d\n",source);
      }

      /* Print results */
      printf("\nResult Matrix:\n");
      for (i = 0; i < A_ROWS; i++)
      {
         printf("\n"); 
         for (j = 0; j < B_COLUMNS; j++)
         { 
            printf("%5.0f   ", Result[i][j]);
         }
      }
      
      //End Time
      double end_time = MPI_Wtime();
      printf("\n Done in %f seconds.\n", end_time - start_time);
   }


   //SLAVE
   if (rank > 0)
   {
      //Receive data from Master
      MPI_Recv( &offset, 1, MPI_INT, 0, FROM_MASTER, MPI_COMM_WORLD, &status );
      
      MPI_Recv( &rows, 1, MPI_INT, 0, FROM_MASTER, MPI_COMM_WORLD, &status );
      
      MPI_Recv( &A, rows * 32, MPI_DOUBLE, 0, FROM_MASTER, MPI_COMM_WORLD, &status );
      
      MPI_Recv( &B, 32 * B_COLUMNS, MPI_DOUBLE, 0, FROM_MASTER, MPI_COMM_WORLD, &status );

      for (k = 0; k < B_COLUMNS; k++)
      {
         for (i = 0; i < rows; i++)
         {
            Result[i][k] = 0.0;
            for (j = 0; j < 32; j++)
            {
               Result[i][k] = Result[i][k] + A[i][j] * B[j][k];
            }
         }
      }
   
      //Send Calculated data to Master
      MPI_Send( &offset, 1, MPI_INT, 0, FROM_SLAVE, MPI_COMM_WORLD );
      
      MPI_Send( &rows, 1, MPI_INT, 0, FROM_SLAVE, MPI_COMM_WORLD );
      
      MPI_Send( &Result, rows * B_COLUMNS, MPI_DOUBLE, 0, FROM_SLAVE, MPI_COMM_WORLD );
   }

   MPI_Finalize();

}