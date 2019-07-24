import axios from 'axios';
import { SECTION } from './SectionHelper';

export default {
  getChartsForSection(sectionParams) {
    let url = `/admin/projects/${sectionParams.projectId}/metrics?numDays=${sectionParams.numDays}&numMonths=${sectionParams.numMonths}&loadDataForFirst=${sectionParams.loadDataForFirst}`;
    if (sectionParams.section !== SECTION.PROJECTS) {
      url = `/admin/projects/${sectionParams.projectId}/${sectionParams.section}/${sectionParams.sectionIdParam}/metrics?numDays=${sectionParams.numDays}&numMonths=${sectionParams.numMonths}&loadDataForFirst=${sectionParams.loadDataForFirst}`;
    }
    return axios.get(url)
      .then(response => Promise.resolve(this.buildCharts(response.data)));
  },

  getChartForSection(sectionParams) {
    let url = `/admin/projects/${sectionParams.projectId}/metrics/${sectionParams.chartBuilderId}?numDays=${sectionParams.numDays}`;
    if (sectionParams.section !== SECTION.PROJECTS) {
      url = `/admin/projects/${sectionParams.projectId}/${sectionParams.section}/${sectionParams.sectionIdParam}/metrics/${sectionParams.chartBuilderId}?numDays=${sectionParams.numDays}&numMonths=${sectionParams.numMonths}`;
    }
    return axios.get(url)
      .then(response => Promise.resolve(this.buildChart(response.data)));
  },


  buildCharts(data) {
    return data.map(item => this.buildChart(item));
  },

  buildChart(chartData) {
    const chartType = (chartData.chartType === 'HorizontalBar' || chartData.chartType === 'VerticalBar') ? 'bar' : chartData.chartType.toLowerCase();
    const hasData = Array.isArray(chartData.dataItems) && chartData.dataItems.length;
    const { dataLoaded } = chartData;
    const series = this.buildSeries(chartData);
    const chartMeta = chartData.chartOptions;
    const options = this.buildOptions(chartData.chartType, chartData.chartOptions);
    return {
      chartType,
      hasData,
      dataLoaded,
      options,
      chartMeta,
      series,
    };
  },

  buildSeries(chartData) {
    let seriesPairs = chartData.dataItems.map(dataItem => ({ x: dataItem.value, y: dataItem.count }));
    if (chartData.chartOptions.sort === 'asc') {
      seriesPairs = seriesPairs.sort((a, b) => a.y - b.y);
    } else if (chartData.chartOptions.sort === 'desc') {
      seriesPairs = seriesPairs.sort((a, b) => b.y - a.y);
    }
    return [{ name: (chartData.chartOptions.dataLabel || ''), data: seriesPairs }];
  },

  buildOptions(chartType, chartOptions) {
    const distributed = !Object.prototype.hasOwnProperty.call(chartOptions, 'distributed') || chartOptions.distributed === true;
    const showDataLabels = Object.prototype.hasOwnProperty.call(chartOptions, 'showDataLabels') && chartOptions.showDataLabels === true;
    const options = {
      chart: {
        id: chartOptions.chartBuilderId,
      },
      dataLabels: { enabled: showDataLabels },
      plotOptions: {},
      title: {
        text: chartOptions.title,
        align: chartOptions.titlePosition || 'left',
        style: {
          fontSize: chartOptions.titleSize || '14px',
          color: chartOptions.titleColor || '#008FFB',
        },
      },
      xaxis: {
        type: chartOptions.xAxisType,
        title: { text: chartOptions.xAxisLabel },
      },
      yaxis: {
        type: chartOptions.yAxisType,
        title: { text: chartOptions.yAxisLabel },
      },
      theme: {
        palette: chartOptions.palette,
      },
    };
    if (chartType === 'HorizontalBar') {
      options.plotOptions.bar = {
        horizontal: true,
        distributed,
        dataLabels: {
          position: chartOptions.dataLabelPosition || 'center',
        },
      };
    } else if (chartType === 'VerticalBar') {
      options.plotOptions.bar = {
        horizontal: false,
        distributed,
        dataLabels: {
          position: chartOptions.dataLabelPosition || 'center',
        },
      };
    }

    return options;
  },
};