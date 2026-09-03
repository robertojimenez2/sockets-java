//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
// ROBERTO DE JESUS JIMENEZ REAL. 24310137. 5O
import java.util.Random;

static class HiloSumador extends Thread {
    private int[] numeros;
    private int inicio;
    private int fin;
    private int sumaParcial;

    public HiloSumador(int[] numeros, int inicio, int fin) {
        this.numeros = numeros;
        this.inicio = inicio;
        this.fin = fin;
        this.sumaParcial = 0;
    }

    @Override
    public void run() {
        for (int i = inicio; i < fin; i++) {
            sumaParcial += numeros[i];
        }
    }

    public int getSumaParcial() {
        return sumaParcial;
    }
}

void main() {
    //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
    // to see how IntelliJ IDEA suggests fixing it.
    int[] numeros = new Random().ints(10000000, 0, 100000).toArray();
//    for(int i = 0; i<numeros.length;i++) {
//        IO.println(numeros[i]);
//    }

    // Calculo de suma con un metodo estructural
    long inicio_s = System.nanoTime();
    int suma = 0;
    for (int i =0; i<numeros.length;i++) {
        suma += numeros[i];
    }
    long fin_s = System.nanoTime();
    long tiempoEjecucion_s = fin_s - inicio_s;

    IO.println("Suma: " + suma);
    IO.println("Tiempo de ejecicion programacion estructurada: " + tiempoEjecucion_s);

    // Calculo de suma con computacion paralela
    long inicio_p = System.nanoTime();
    int numeroDeHilos = 3; // lo puedes modificar
    int tamanoBloque = numeros.length / numeroDeHilos;
    HiloSumador[] hilos = new HiloSumador[numeroDeHilos];

            // Crear y arrancar los hilos
    for (int i = 0; i < numeroDeHilos; i++) {
        int inicio = i * tamanoBloque;
        // Si es el ultimo hilo, que llegue hasta el final del arreglo por si no es división exacta
        int fin = (i == numeroDeHilos - 1) ? numeros.length : (i + 1) * tamanoBloque;

        hilos[i] = new HiloSumador(numeros, inicio, fin);
        hilos[i].start(); // start() ejecuta el método run() en un nuevo hilo
    }

    int sumaTotal = 0;

    try {
        for (int i = 0; i < numeroDeHilos; i++) {
            hilos[i].join(); // Pausa el programa principal hasta que este hilo termine
            sumaTotal += hilos[i].getSumaParcial();
        }
    } catch (InterruptedException e) {
        System.out.println("Un hilo fue interrumpido.");
    }
    long fin_p = System.nanoTime();
    long tiempoEjecucion_p = fin_p - inicio_p;
    IO.println("Suma: " + sumaTotal);
    IO.println("Tiempo de ejecicion programacion paralela: " + tiempoEjecucion_p);

    // usando parallel strams
    long inicioParallel = System.nanoTime();
    int sumaParallel = Arrays.stream(numeros)
            .parallel()
            .sum();
    long finParallel = System.nanoTime();
    long tiempoParallel = finParallel - inicioParallel;
    IO.println("Suma: " + sumaParallel);
    IO.println("Tiempo de ejecucion con el metodo parallel strams: " + tiempoParallel);
}
