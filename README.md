## Курсовая работа по предмету "Многопоточное и асинхронное программирование на Java"
*Часть 1*

## Thread Pool

*Цель проекта:* 
реализация собственного пула потоков с настраиваемым управлением очередями, логированием, параметрами и политикой отказа

## Быстрый старт

1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/Jaba-Klava/ThreadPool.git
   ```
2. Запустите демо-версию:
   ```bash
   cd ThreadPool
   mvn compile
   java -cp target/classes org.example.Main
   ```

## Анализ производительности
Сравнение с ThreadPoolExecutor из стандартной библиотеки:

1. Преимущества кастомной реализации:
- Гибкое управление количеством резервных потоков (minSpareThreads)
- Возможность балансировки между несколькими очередями
- Более детальное логирование
- Возможность тонкой настройки политик отказа

2. Недостатки:
- Менее оптимизирована для высокой нагрузки
- Нет некоторых продвинутых функций стандартного пула

## Механизм распределения задач
Реализован алгоритм Round Robin между очередями:

1. Новая задача последовательно предлагается очередям
2. Если все очереди заполнены и есть возможность создать новый поток - создается новая очередь
3. При перегрузке применяется выбранная политика отказа

## Производительность (AMD Ryzen 5 3600, 6C/12T, 16GB RAM)

| Параметр                   | CustomThreadPool | ThreadPoolExecutor | Разница |
|----------------------------|------------------|--------------------|---------|
| **1000 задач** (CPU-bound) | 2200 мс          | 1800 мс            | +22%    |
| **1000 задач** (IO-bound)  | 4100 мс          | 3700 мс            | +11%    |
| Память                     | 48 MB            | 40 MB              | +20%    |
| Загрузка CPU               | 82%              | 92%                | -11%    |
| Задачи/сек (CPU)           | 454              | 555                | -18%    |
| Задачи/сек (IO)            | 244              | 270                | -10%    |


## Оптимальные настройки (для AMD Ryzen 5 3600)

|  Сценарий	 | corePoolSize | maxPoolSize | queueSize | KeepAlive |
|------------|--------------|-------------|-----------|-----------|
|Вычисления  |	     6	    |     12	   |  50-100	|    30s    |
|Веб-запросы |	     12	    |     24	   |  200-500	|    60s    |

## Алгоритм балансировки

Реализован Round Robin с адаптацией:
1. Новые задачи распределяются по очередям циклически
2. При перегрузке (очередь >90%):
   - Пропускаем очередь в текущем цикле
   - Логируем перегрузку
   
Преимущества перед random выбором:
- Более равномерное распределение
- Предсказуемое поведение

## Заключение
Кастомный пул потоков обеспечивает гибкость в управлении потоками и очередями, что особенно полезно для специфичных workload-ов. Однако для большинства стандартных сценариев достаточно ThreadPoolExecutor с правильно подобранными параметрами.

## Контакты

Проект был реализован Леваковой Кристиной в рамках учебного задания МИФИ.
