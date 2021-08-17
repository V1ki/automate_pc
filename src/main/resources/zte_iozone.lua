-- command 为自动化测试内置的库. 目前包含 iozone ,pull 命令, 后续会支持shell 命令
-- log 为内置的日志输出命令,会将日志输出至文件及控制台.  log 有两个参数,第一个为tag, 第二个为msg. tag是为了后续进行筛选使用的.
-- version: 0.0.1
require 'command'

log("iozone", "Start iozone test!")

testItemPercent = {70, 15, 4, 2, 1, 1, 2, 1, 4}
testItemBs  = {4, 8,  12 , 16,  20 , 32,  36 , 40 , 512}
-- testItemPercent = {4,70, 15, 4, 2, 1, 1, 2, 1}
-- testItemBs  = {512,4, 8,  12 , 16,  20 , 32,  36 , 40 }

availableCap = command.shell("df |  grep '/data/media' | head -n 1  | tr -s ' ' | cut -d ' ' -f 4")
log('iozone', string.format('availableCap:%d!',availableCap))
availableCap = availableCap/1024 - 400

emmcSize = command.shell("df | grep '/data/media' | head -n 1  | tr -s ' ' | cut -d ' ' -f 2")
emmcSize = emmcSize / 1024
totalPercent = 0
TBWSize = 0
currTestCap = 0

writeCount = 0

if 8192 > emmcSize then
    TBWSize = 8 * 1024 * 1024
elseif 16384 > emmcSize then
    TBWSize = 10 * 1024 * 1024
else
    TBWSize = 15 * 1024 * 1024
end


function humanSize(s)
    suffixes = {'MB', 'GB', 'TB' }
    for i, suffix in pairs(suffixes) do
        if s < 1024 then
            return string.format("%d %s",s, suffix)
        end
        s = s / 1024
     end

    return string.format("%d TB",count)
end

for i = 1, #testItemPercent do
    totalPercent = testItemPercent[i] * testItemBs[i] + totalPercent
    print(totalPercent)
    print(testItemBs[i])
end

log('iozone', string.format('TBWSize:%d!',TBWSize))
log('iozone', string.format('emmcSize:%d!',emmcSize))
log('iozone', string.format('availableCap:%d!',availableCap))


log('iozone', string.format('prepare test!  Already Write:%s ',humanSize(writeCount)))
local earlyBreak = false
while true do

    command.shell("rm /storage/emulated/0/*.io")
    availableCap = command.shell("df |  grep '/data/media' | head -n 1  | tr -s ' ' | cut -d ' ' -f 4")
    availableCap = availableCap/1024 - 400

    for i=1,#testItemPercent do
        log('iozone', string.format('start No.%d test!',i))
        local itemCap = availableCap * testItemPercent[i] * testItemBs[i] / totalPercent

        local options = string.format('-w -i0 -i2 -r%dk -s%dm -f /storage/emulated/0/%d_seq.io',testItemBs[i], itemCap ,testItemBs[i])
        if itemCap > 4000 then
            local cycle = itemCap / 4000
            log('iozone', string.format(' cycle:.%d test itemCap :%d!',cycle,itemCap))
            local itemBS = testItemBs[i]


            for i=1,cycle ,1 do
                command.shell(string.format('touch /storage/emulated/0/%d_%d_seq.io',itemBS, i))
                options = string.format('-w -i0 -i2 -r%dk -s4000m -f /storage/emulated/0/%d_%d_seq.io',itemBS, itemBS,i )
                log('iozone', options)
                log('iozone', command.iozone(options))
                itemCap = itemCap - 4000
            end

            if itemCap > 0 then

                command.shell(string.format('touch /storage/emulated/0/%d_%d_seq.io',itemBS, cycle + 1))
                options = string.format('-w -i0 -i2 -r%dk -s%dm -f /storage/emulated/0/%d_%d_seq.io',itemBS,  itemCap, itemBS, cycle + 1 )
                log('iozone', options)
                log('iozone', command.iozone(options))
            end

            log('iozone', string.format(' lastCap :%d!',itemCap))

        else
            log('iozone', options)
            log('iozone', command.iozone(options))
        end


        writeCount = writeCount + itemCap * 3
        ----    command.pull("/storage/emulated/0/test.bin", "/Users/v1ki/IdeaProjects/automate_pc/results")
        log('iozone', string.format('No.%d test completed !  Already Write:%s ',i,humanSize(writeCount)))
        currTestCap = currTestCap + itemCap * 3
        if currTestCap >= TBWSize then
            earlyBreak = true
            break
        end
    end
    log("iozone", "iozone test completed!")
    if earlyBreak then
        break
    end
end

log("iozone", "iozone test completed!")