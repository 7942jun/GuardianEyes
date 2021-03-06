from matplotlib import pyplot as plt
import csv
from absl import app, flags

FLAGS = flags.FLAGS

flags.DEFINE_string('name', None, "File name")

def main(argv):
    if FLAGS.name is None:
        print("Need file name. Use --name flag")
        return
    
    data = []
    _min = 10000
    _max = -10000
    with open(FLAGS.name, newline='') as f:
        rows = csv.reader(f)
        for row in rows:
            for d in row:
                _min = min(_min, float(d))
                _max = max(_max, float(d))
            data.append(row)

    num_rows = len(data)
    avg_seq = [[] for i in range(len(data[0]))]
    avg_buf = [0 for i in range(len(data[0]))]

    for i in range(len(data[0])):
        count = 10
        #plt.plot([float(row[i]) for row in data], 'o', markersize=2, label=str(i))

        for j, row in enumerate(data):
            avg_buf[i] += float(row[i])
            if j % count == 0:
                avg_buf[i] /= count
                avg_seq[i].append(avg_buf[i])

            frame_num = range(0, len(data), count)

       # print(len(avg_seq[i]))
       # print(len(frame_num))
        plt.plot(frame_num, avg_seq[i],'o',markersize=5, label="avg:"+str(i))



    plt.ylim([_min - 1, _max + 1])
    plt.ylabel('y-coordinate(m)')
    plt.xlabel('frame')
    plt.legend()
    plt.show()


if __name__  == '__main__':
    app.run(main)
