import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Simulacion {
    private FileInputStream entrada = null;
    private Scanner lector = null;
    private Planificador planificador;
    private Memoria memoria;
    private CacheDatosC cachedc;
    private CacheDatosD cachedd;
    private CacheInstrucciones cachei0;
    private CacheInstrucciones cachei1;
    private Nucleo0 nucleo0;
    private Nucleo1 nucleo1;

    private int numHilillos;
    private int quantum;
    int inicioInstrucciones;

    public Simulacion(){
        lector = new Scanner(System.in);

        System.out.println("Digite el número de hilillos que van a correr en la simulación.");
        numHilillos = lector.nextInt();

        System.out.println("Digite el valor del quantum para la simulación.");
        quantum = lector.nextInt();

        memoria = new Memoria();
        planificador = new Planificador();
        inicioInstrucciones = 384;
    }

    public void empezarSimulacion(){
        lectorCarpeta();

        nucleo0.start();
        nucleo1.start();

        try {
            nucleo0.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            nucleo1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        print();
    }

    public void lectorCarpeta(){
        File carpeta = new File("/home/nathan/Simulacion_Arqui/SimulacionArquitectura/src/ArchivosSimulacion"); // Se debe cambiar según el lugar de la carpeta

        if (carpeta.exists()) {
            File[] archivos = carpeta.listFiles();
            int numMinHilillos;

            // Elije la menor cantidad de hilillos que puede aceptar
            if(numHilillos < archivos.length)
                numMinHilillos = numHilillos;
            else
                numMinHilillos = archivos.length;

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

    public void cargaPcb(String name){
        Pcb nuevo = new Pcb(name, inicioInstrucciones);
        int[] instruccion = new int[4];

        while (lector.hasNextLine()) {
            String linea = lector.nextLine();
            String[] digito = linea.split(" ");

            for(int i = 0; i < instruccion.length; ++i)
                instruccion[i] = Integer.parseInt(digito[i]);

            memoria.escribirInstruccion(inicioInstrucciones, instruccion);
            inicioInstrucciones += 4;
        }

        // TODO IMPORTANTE por ahora esto esta metiendo las cosas en la cola de listos para probar
        // TODO RECORDAR que lo meta en la cola correspondiente para que todos funcionen bien
        planificador.agregarProcesosTerminados(nuevo);
    }

    private void print(){
        planificador.print();
        memoria.print();
        //Faltan los dos caches de datos
    }
}
