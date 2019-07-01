import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;

public class Simulacion {
    private FileInputStream entrada = null;
    private Scanner lector = null;
    private Planificador planificador;
    private Memoria memoria;
    private CacheDatosC cachedc;
    private CacheDatosD cachedd;
    private CacheInstrucciones cachei0;
    private CacheInstrucciones cachei1;
    private Nucleo nucleo0;
    private Nucleo nucleo1;
    private ReentrantLock lockDatosCache0;
    private ReentrantLock lockDatosCache1;
    private ReentrantLock lockMemoriaDatos;
    private CyclicBarrier barrera;

    private int numHilillos;
    private int quantum;
    int inicioInstrucciones = 384;

    public Simulacion(){
        lector = new Scanner(System.in);

        // Se toman los valores de entrada
        System.out.println("Digite el número de hilillos que van a correr en la simulación.");
        numHilillos = lector.nextInt();

        System.out.println("Digite el valor del quantum para la simulación.");
        quantum = lector.nextInt();

        // Se inicializan variables
        memoria = new Memoria();
        cachei0 = new CacheInstrucciones(memoria);
        cachei1 = new CacheInstrucciones(memoria);
        cachedc = new CacheDatosC(memoria);
        cachedd = new CacheDatosD(memoria);
        cachedc.setOtraCache(cachedd);
        cachedd.setOtraCache(cachedc);
        planificador = new Planificador();
        lockDatosCache0 = new ReentrantLock();
        lockDatosCache1 = new ReentrantLock();
        lockMemoriaDatos = new ReentrantLock();
        barrera = new CyclicBarrier(2,null);
    }

    /**
     * Se inicializa cada uno de los núcleos y el proceso principal esperará hasta que cada uno de los núcleos acabe
     * para imprimir en pantalla toda la información importante de la simulación
     */
    public void empezarSimulacion(){
        //lectorCarpeta();

        Nucleo0 n0 = new Nucleo0(cachei0, cachedd, cachedc, 4, planificador, lockDatosCache0, lockDatosCache1, lockMemoriaDatos, barrera);

        n0.run();
        //nucleo1.start();

        try {
            nucleo0.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /*try {
            nucleo1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        print();
    }

    /**
     * Se busca la carpeta en donde están los archivos del código para la simulación y empieza a procesar a cada uno
     */
    public void lectorCarpeta(){
        // Se debe cambiar según el lugar de la carpeta
        File carpeta = new File("/home/nathan/Simulacion_Arqui/SimulacionArquitectura/src/ArchivosSimulacion");

        // Si la carpeta existe cree los "hilillos"
        if (carpeta.exists()) {
            File[] archivos = carpeta.listFiles();
            int numMinHilillos;

            // Elije la menor cantidad de hilillos que puede aceptar
            if(numHilillos < archivos.length)
                numMinHilillos = numHilillos;
            else
                numMinHilillos = archivos.length;

            // Crea un PCB para cada uno de los "hilillos"
            for(int i = 0; i < numMinHilillos; ++i){
                if(!archivos[i].isDirectory()){
                    try {
                        entrada = new FileInputStream(archivos[i].getAbsolutePath());
                        lector = new Scanner(entrada, "UTF-8");
                        cargaPcb(archivos[i].getName());
                    } catch (FileNotFoundException e) {
                        System.out.println("El archivo " + archivos[i].getAbsolutePath() + " no existe.");
                        System.exit(-1);
                    }
                }
            }
        }
    }

    /**
     * Crea un nuevo PCB y almacena en la memoria de instrucciones cada una de las instrucciones contenidas en el archivo
     * @param name
     */
    public void cargaPcb(String name){
        Pcb nuevo = new Pcb(name, inicioInstrucciones);
        int[] instruccion = new int[4];

        // Lee cada una de las filas del archivo y las guarda como un bloque en la memoria de instrucciones
        while (lector.hasNextLine()) {
            String linea = lector.nextLine();
            String[] digito = linea.split(" ");

            for(int i = 0; i < instruccion.length; ++i)
                instruccion[i] = Integer.parseInt(digito[i]);

            memoria.escribirInstruccion(inicioInstrucciones, instruccion);
            inicioInstrucciones += 4;
        }

        // Se agrega en la cola de procesos restantes
        planificador.agregarProcesosRestantes(nuevo);
    }

    /**
     * Imprime en pantalla toda la información importante
     */
    private void print(){
        planificador.print();
        memoria.print();
        cachedd.print();
        cachedc.print();
    }
}
